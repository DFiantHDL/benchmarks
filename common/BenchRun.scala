// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks

import dfhdl.internals.NoTopAnnotIsRequired

/** Runs every DFHDL benchmark (SERV, SHA farm, protocol engine) end to end: each commits its
  * Verilog and reports the DFacsimile throughput plus architectural state. Pass `--verilator` to
  * also build and run the external Verilator model of each top and print its reference measurement
  * right after the matching DFacsimile line (bit-exact state lines are the cross-simulator check).
  * Run with:
  *
  * `benchmarks/runMain dfhdl.benchmarks.benchRun [--verilator]`
  */
object benchRun extends NoTopAnnotIsRequired:
  private def section(title: String): Unit =
    println()
    println(s"========== $title ==========")

  def main(args: Array[String]): Unit =
    BenchTable.defer() // accumulate all three suites into one combined summary table
    section("SERV / Servant (bit-serial RISC-V)")
    dfhdl.benchmarks.serv.servBench.main(args)
    section("SHA farm (wide datapath)")
    dfhdl.benchmarks.sha_farm.shaFarmBench.main(args)
    section("Protocol engine")
    dfhdl.benchmarks.protocol_engine.protocolEngineBench.main(args)
    section("Summary")
    BenchTable.printAndClear()
    if !args.contains("--verilator") then
      println()
      println("Re-run with --verilator to add the Verilog reference measurements.")
end benchRun
