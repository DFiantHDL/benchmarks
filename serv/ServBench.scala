// Benchmark mains for the SERV/servant port. Written fresh for the DFHDL benchmarks
// repository.
//
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*
import dfhdl.sim.*
import dfhdl.benchmarks.{hex, Verilator, BenchTable}
import dfhdl.internals.NoTopAnnotIsRequired

// DFacsimile applies register/memory inits at time zero (the reset values), so no explicit reset
// preamble is needed; the reset magnet stays deasserted and the design starts from its init state.
private def resetAndRun(run: SimulationRun[? <: servant_sim], cycles: Long): Unit =
  if (cycles > 0) run.continue(cycles)

private def stateLine(run: SimulationRun[? <: servant_sim], total: Long): String =
  run.inspect { dut =>
    f"  after $total%,d cycles: chars=${dut.char_count.peek.toScalaBigInt}" +
      s" lines=${dut.line_count.peek.toScalaBigInt}" +
      s" sig=${hex(dut.char_sig.peek.uint.toScalaBigInt, 8)}" +
      s" last=${hex(dut.last_char.peek.uint.toScalaBigInt, 2)}" +
      s" halts=${dut.halt_count.peek.toScalaBigInt}" +
      s" memacks=${dut.mem_ack_count.peek.toScalaBigInt}" +
      s" pc=${hex(dut.pc_adr.peek.uint.toScalaBigInt, 8)}"
  }

/** Commit the Verilog for all servant tops (for the external Verilator harness), then run the
  * DFacsimile side on every top and print throughput plus the architectural state line. Both RAM
  * sizes run: the memory node (a `long[]` backing store with O(1) read/write) replaced the
  * packed-bits dynamic-shift lowering, so the 32 KiB tops run at the same speed as the mini. Run
  * with:
  *
  * `benchmarks/runMain dfhdl.benchmarks.serv.servBench`
  */
object servBench extends NoTopAnnotIsRequired:
  private val harness = "benchmarks/serv/verilator/bench_serv.cpp"
  private var withVerilator = false

  private def bench(
      name: String,
      mkTop: () => servant_sim,
      top: String,
      tier: SimTier,
      warmup: Long,
      cycles: Long
  ): Unit =
    val run = mkTop().simulation.withTier(tier).run()
    resetAndRun(run, warmup)
    val t0 = System.nanoTime()
    run.continue(cycles)
    val dt = (System.nanoTime() - t0) / 1e9
    val mcps = cycles / dt / 1e6
    val state = stateLine(run, warmup + cycles)
    println(f"[$name $tier] timed $cycles%,d cycles in $dt%.3f s = $mcps%.3f Mcycles/s")
    println(state)
    val v =
      if withVerilator && tier == SimTier.Codegen then Verilator.run(top, harness, warmup, cycles)
      else None
    if tier == SimTier.Codegen then
      BenchTable.add(
        s"serv/$name",
        mcps,
        BenchTable.field("sig", state),
        v.map(_._1),
        v.map(t => BenchTable.field("sig", t._2))
      )
  end bench

  def main(args: Array[String]): Unit =
    withVerilator = args.contains("--verilator")
    ServantHello().compile
    ServantPhil().compile
    ServantHelloMini().compile
    println("committed Verilog to sandbox/ServantHello, ServantPhil, ServantHelloMini")
    bench(
      "hello-mini",
      () => ServantHelloMini(),
      "ServantHelloMini",
      SimTier.Codegen,
      100_000L,
      2_000_000L
    )
    bench(
      "hello-mini",
      () => ServantHelloMini(),
      "ServantHelloMini",
      SimTier.Interpreter,
      5_000L,
      50_000L
    )
    bench("hello-32k", () => ServantHello(), "ServantHello", SimTier.Codegen, 100_000L, 2_000_000L)
    bench("phil-32k", () => ServantPhil(), "ServantPhil", SimTier.Codegen, 100_000L, 10_000_000L)
    BenchTable.flush()
  end main
end servBench

/** Temporary bring-up scaffolding: single-step the mini top and trace the fetch/RF handshake. Run
  * with:
  *
  * `benchmarks/runMain dfhdl.benchmarks.serv.servTrace [cycles]`
  */
object servTrace extends NoTopAnnotIsRequired:
  def main(args: Array[String]): Unit =
    val cycles = args.headOption.map(_.toInt).getOrElse(90)
    val run = ServantHelloMini().simulation.withTier(SimTier.Codegen).run()
    resetAndRun(run, 0)
    for c <- 1 to cycles do
      run.continue(1)
      val line = run.inspect { dut =>
        val core = dut.soc.cpu.cpu
        val rfif = dut.soc.cpu.rf_ram_if
        s"c=$c" +
          s" icyc=${core.o_ibus_cyc.peek.bits.uint.toScalaBigInt}" +
          s" iadr=${hex(core.o_ibus_adr.peek.uint.toScalaBigInt, 8)}" +
          s" ack=${dut.soc.ram.o_wb_ack.peek.bits.uint.toScalaBigInt}" +
          s" rdy=${rfif.o_ready.peek.bits.uint.toScalaBigInt}" +
          s" clsb=${core.state.cnt_lsb.peek.uint.toScalaBigInt}" +
          s" cnt=${core.state.cnt.peek.toScalaBigInt}" +
          s" cen=${core.state.o_cnt_en.peek.bits.uint.toScalaBigInt}" +
          s" iste=${core.state.o_init.peek.bits.uint.toScalaBigInt}" +
          s" pcen=${core.state.o_ctrl_pc_en.peek.bits.uint.toScalaBigInt}" +
          s" tso=${core.state.i_two_stage_op.peek.bits.uint.toScalaBigInt}" +
          s" idone=${core.state.init_done.peek.bits.uint.toScalaBigInt}" +
          s" opc=${core.decode.opcode.peek.uint.toScalaBigInt}"
      }
      println(line)
    end for
  end main
end servTrace

/** Functional verification: run the mini hello top and reconstruct the UART text from the monitor's
  * character counter, printing the decoded output. With the `edges` argument it instead dumps the
  * first UART edge intervals (for calibrating cycles-per-bit). Run with:
  *
  * `benchmarks/runMain dfhdl.benchmarks.serv.servText [cycles|edges]`
  */
object servText extends NoTopAnnotIsRequired:
  def main(args: Array[String]): Unit =
    val run = ServantHelloMini().simulation.withTier(SimTier.Codegen).run()
    resetAndRun(run, 0)
    if (args.contains("edges"))
      var last = false
      var lastCycle = 0L
      var edges = 0
      var cycle = 0L
      while edges < 40 && cycle < 200_000 do
        run.continue(1)
        cycle += 1
        val cur = run.inspect { dut => dut.q.peek.bits.uint.toScalaBigInt == 1 }
        if (cur != last)
          println(s"cycle $cycle: q=${if cur then 1 else 0} (+${cycle - lastCycle})")
          last = cur
          lastCycle = cycle
          edges += 1
    else
      val cycles = args.headOption.map(_.toLong).getOrElse(300_000L)
      val sb = new StringBuilder
      var seen = BigInt(0)
      var advanced = 0L
      while advanced < cycles do
        run.continue(100)
        advanced += 100
        run.inspect { dut =>
          val cc = dut.char_count.peek.toScalaBigInt
          if (cc > seen)
            sb += dut.last_char.peek.uint.toScalaBigInt.toInt.toChar
            seen = cc
        }
      println(s"decoded $seen chars:")
      println(sb.result())
      println(stateLine(run, advanced))
    end if
  end main
end servText
