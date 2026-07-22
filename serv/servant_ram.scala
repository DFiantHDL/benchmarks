// DFHDL port of servant_ram.v from the Servant SoC by Olof Kindgren (see serv_top.scala
// for the port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2019 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** servant_ram.v: the firmware/data RAM (32-bit words, byte enables, single-cycle ack), preloaded
  * from a Verilog hex file via `initFile`. `depth` is the size in bytes. Clocked on `i_wb_clk` with
  * the synchronous MINI reset `i_wb_rst` (which resets only the ack flop). The memory lives in a
  * nested `RTDomain` that is `@timing.related` to this design (shares its clock) but carries a
  * clock-only constraint (no reset), so the memory is excluded from the module reset and keeps its
  * `initFile` power-up contents.
  */
@hw.constraints.timing.clock(portName = "i_wb_clk")
@hw.constraints.timing.reset(portName = "i_wb_rst")
class servant_ram(
    val memfile: String = "benchmarks/serv/sw/hello_uart.hex",
    val depth: Int <> CONST = 256
) extends RTDesign:
  self =>
  val words = depth.toScalaInt / 4
  val i_wb_adr = Bits.until(words) <> IN
  val i_wb_dat = Bits(32) <> IN
  val i_wb_sel = Bits(4) <> IN
  val i_wb_we = Bit <> IN
  val i_wb_cyc = Bit <> IN
  val o_wb_rdt = Bits(32) <> OUT.REG init all(0)
  val o_wb_ack = Bit <> OUT.REG init 0

  o_wb_ack.din := i_wb_cyc && !o_wb_ack

  @hw.constraints.timing.related(self, includeReset = false)
  @hw.annotation.flattenMode.transparent
  val write = new RTDomain:
    val mem = Bits(32) X words <> VAR.REG initFile memfile
    val we = i_wb_we && i_wb_cyc
    if (we && i_wb_sel(0)) mem(i_wb_adr)(7, 0).din := i_wb_dat(7, 0)
    if (we && i_wb_sel(1)) mem(i_wb_adr)(15, 8).din := i_wb_dat(15, 8)
    if (we && i_wb_sel(2)) mem(i_wb_adr)(23, 16).din := i_wb_dat(23, 16)
    if (we && i_wb_sel(3)) mem(i_wb_adr)(31, 24).din := i_wb_dat(31, 24)

  o_wb_rdt.din := write.mem(i_wb_adr)
end servant_ram
