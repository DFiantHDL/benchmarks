// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks

import dfhdl.*
import dfhdl.internals.NoTopAnnotIsRequired

/** Measures how the Verilator reference itself scales with `--threads`, to size the payoff of
  * parallelism before investing in a parallel DFacsimile kernel. Commits the Verilog for the two
  * parallelizable tops (the 64-core SHA farm and the protocol engine), then verilates + runs each
  * at a sweep of thread counts, printing single-thread and multithreaded Mcps side by side. Run:
  * `benchmarks/runMain dfhdl.benchmarks.verilatorThreadSweep [threadCounts...]`  (default 1 2 4 8 16)
  */
object verilatorThreadSweep extends NoTopAnnotIsRequired:
  private val shaHarness = "benchmarks/sha_farm/verilator/bench_sha.cpp"
  private val protoHarness = "benchmarks/protocol_engine/verilator/bench_protocol_engine.cpp"

  def main(args: Array[String]): Unit =
    val warmup = 1_000_000L
    val timed = 10_000_000L
    val threadCounts = args.flatMap(_.toIntOption).toSeq match
      case s if s.nonEmpty => s
      case _               => Seq(1, 2, 4, 8, 16)

    dfhdl.benchmarks.sha_farm.SHAFarm64().compile
    dfhdl.benchmarks.protocol_engine.ProtocolEngine().compile
    println("committed Verilog to sandbox/SHAFarm64, sandbox/ProtocolEngine")

    val rows = for n <- threadCounts yield
      Verilator.threads = n
      val sha = Verilator.run("SHAFarm64", shaHarness, warmup, timed).map(_._1)
      val proto = Verilator.run("ProtocolEngine", protoHarness, warmup, timed).map(_._1)
      (n, sha, proto)

    def cell(o: Option[Double]): String = o.map(m => f"$m%8.2f").getOrElse("     -  ")
    val shaBase = rows.headOption.flatMap(_._2)
    val protoBase = rows.headOption.flatMap(_._3)
    def speedup(cur: Option[Double], base: Option[Double]): String =
      (cur, base) match
        case (Some(c), Some(b)) if b > 0 => f"${c / b}%5.2fx"
        case _                           => "   -  "
    println()
    println(f"${"threads"}%7s | ${"sha-64 Mcps"}%12s | ${"vs 1T"}%6s | ${"proto Mcps"}%11s | ${"vs 1T"}%6s")
    println("-".repeat(56))
    for (n, sha, proto) <- rows do
      println(
        f"$n%7d | ${cell(sha)}%12s | ${speedup(sha, shaBase)}%6s | ${cell(proto)}%11s | ${speedup(proto, protoBase)}%6s"
      )
    println("\nverilator thread sweep complete")
  end main
end verilatorThreadSweep
