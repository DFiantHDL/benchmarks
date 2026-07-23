// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.sha_farm

import dfhdl.*
import dfhdl.sim.*
import dfhdl.benchmarks.{hex, Verilator, BenchTable}
import dfhdl.internals.NoTopAnnotIsRequired

/** The DFacsimile side of the scaled [[SHAFarm]] baseline benchmark: commit the generated Verilog
  * (for the external Verilator harness), then per kernel tier warm up (JIT), time a bulk block-less
  * `continue`, and print the final architectural state for cross-simulator equivalence checking.
  * Run with:
  *
  * `benchmarks/runMain dfhdl.benchmarks.sha_farm.shaFarmBench`
  */
object shaFarmBench extends NoTopAnnotIsRequired:
  private val harness = "benchmarks/sha_farm/verilator/bench_sha.cpp"
  private var withVerilator = false

  // the named top classes carry the committed Verilog; `SHAFarm(n)` is DFacsimile-only
  private def topOf(n: Int): String = n match
    case 64 => "SHAFarm64"
    case 8  => "SHAFarm8"
    case 1  => "SHAFarm1"
    case _  => "SHAFarm"

  private def bench(n: Int, tier: SimTier, warmup: Long, cycles: Long): Unit =
    val run = SHAFarm(n).simulation.withTier(tier).run()
    run.continue(warmup)
    val t0 = System.nanoTime()
    run.continue(cycles)
    val dt = (System.nanoTime() - t0) / 1e9
    val total = warmup + cycles
    val mcps = cycles / dt / 1e6
    val state = run.inspect { dut =>
      f"  after $total%,d cycles: agg=${hex(dut.agg.peek.uint.toScalaBigInt, 32)}"
    }
    println(f"[n=$n%2d $tier] timed $cycles%,d cycles in $dt%.3f s = $mcps%.3f Mcycles/s")
    println(state)
    val v =
      if withVerilator && tier == SimTier.Codegen then
        Verilator.run(topOf(n), harness, warmup, cycles)
      else None
    if tier == SimTier.Codegen then
      BenchTable.add(
        s"sha/n=$n",
        mcps,
        BenchTable.field("agg", state),
        v.map(_._1),
        v.map(t => BenchTable.field("agg", t._2))
      )
  end bench

  def main(args: Array[String]): Unit =
    withVerilator = args.contains("--verilator")
    Verilator.configure(args)
    SHAFarm().compile
    SHAFarm64().compile
    SHAFarm8().compile
    SHAFarm1().compile
    println("committed Verilog to sandbox/SHAFarm, SHAFarm64, SHAFarm8, SHAFarm1")
    bench(32, SimTier.Codegen, 1_000_000L, 10_000_000L)
    bench(32, SimTier.Interpreter, 20_000L, 200_000L)
    // farm-size sweep to locate the codegen scaling cliff
    bench(1, SimTier.Codegen, 1_000_000L, 10_000_000L)
    bench(8, SimTier.Codegen, 250_000L, 2_500_000L)
    bench(64, SimTier.Codegen, 1_000_000L, 5_000_000L)
    BenchTable.flush()
  end main
end shaFarmBench

/** A long single-config run of the farm for profiling (fork with `-XX:StartFlightRecording=...` and
  * aggregate the execution samples). Optional arguments: farm size (default 32) and timed cycle
  * count (default 30M). Run with:
  *
  * `benchmarks/runMain dfhdl.benchmarks.sha_farm.shaProfile [n] [cycles]`
  */
object shaProfile extends NoTopAnnotIsRequired:
  def main(args: Array[String]): Unit =
    val n = if args.nonEmpty then args(0).toInt else 32
    val cycles = if args.length > 1 then args(1).toLong else 30_000_000L
    val run = SHAFarm(n).simulation.withTier(SimTier.Codegen).run()
    run.continue(1_000_000L)
    val t0 = System.nanoTime()
    run.continue(cycles)
    val dt = (System.nanoTime() - t0) / 1e9
    println(f"profiled n=$n $cycles%,d cycles in $dt%.3f s = ${cycles / dt / 1e6}%.2f Mcycles/s")
    println(run.inspect { dut =>
      s"  agg=${hex(dut.agg.peek.uint.toScalaBigInt, 32)}"
    })
end shaProfile
