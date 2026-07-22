// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.protocol_engine

import dfhdl.*
import dfhdl.sim.*
import dfhdl.benchmarks.{hex, Verilator, BenchTable}
import dfhdl.internals.NoTopAnnotIsRequired

/** The DFacsimile side of the [[ProtocolEngine]] benchmark: commit the generated Verilog (for the
  * external Verilator harness), then per kernel tier warm up (JIT), time a bulk block-less
  * `continue`, and print the final architectural state for cross-simulator equivalence checking.
  * Run with:
  *
  * `benchmarks/runMain dfhdl.benchmarks.protocol_engine.protocolEngineBench`
  */
object protocolEngineBench extends NoTopAnnotIsRequired:
  private val harness = "benchmarks/protocol_engine/verilator/bench_protocol_engine.cpp"
  private var withVerilator = false

  private def bench(tier: SimTier, warmup: Long, cycles: Long): Unit =
    val run = ProtocolEngine().simulation.withTier(tier).run()
    run.continue(warmup)
    val t0 = System.nanoTime()
    run.continue(cycles)
    val dt = (System.nanoTime() - t0) / 1e9
    val total = warmup + cycles
    val mcps = cycles / dt / 1e6
    val state = run.inspect { dut =>
      f"  after $total%,d cycles: packets=${dut.packets.peek.toScalaBigInt} " +
        s"drops=${dut.drops.peek.toScalaBigInt} " +
        s"sig=${hex(dut.sig.peek.uint.toScalaBigInt, 8)} " +
        s"trail=${hex(dut.trail.peek.uint.toScalaBigInt, 32)} " +
        s"lfsr=${hex(dut.lfsr.peek.uint.toScalaBigInt, 4)} " +
        s"phase=${dut.phase.peek.bits.uint.toScalaBigInt} " +
        s"beat=${dut.beat.peek.bits.uint.toScalaBigInt}"
    }
    println(f"[$tier] timed $cycles%,d cycles in $dt%.3f s = $mcps%.2f Mcycles/s")
    println(state)
    val v =
      if withVerilator && tier == SimTier.Codegen then
        Verilator.run("ProtocolEngine", harness, warmup, cycles)
      else None
    if tier == SimTier.Codegen then
      BenchTable.add(
        "proto",
        mcps,
        BenchTable.field("sig", state),
        v.map(_._1),
        v.map(t => BenchTable.field("sig", t._2))
      )
  end bench

  def main(args: Array[String]): Unit =
    withVerilator = args.contains("--verilator")
    ProtocolEngine().compile
    println("committed Verilog to sandbox/ProtocolEngine")
    bench(SimTier.Codegen, 2_000_000L, 100_000_000L)
    bench(SimTier.Interpreter, 100_000L, 5_000_000L)
    BenchTable.flush()
  end main
end protocolEngineBench
