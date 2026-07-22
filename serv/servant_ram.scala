// DFHDL port of servant_ram.v from the Servant SoC by Olof Kindgren (see serv_top.scala
// for the port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2019 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** servant_ram.v: the firmware/data RAM (32-bit words, byte enables, single-cycle ack), preloaded
  * from a Verilog hex file via `initFile`.
  */
class servant_ram(val memfile: String, val words: Int) extends RTDesign:
  private def log2(n: Int): Int = 31 - Integer.numberOfLeadingZeros(n)
  val wb_rst = Bit <> IN
  val wb_adr = Bits(log2(words)) <> IN
  val wb_dat = Bits(32) <> IN
  val wb_sel = Bits(4) <> IN
  val wb_we = Bit <> IN
  val wb_cyc = Bit <> IN
  val rdt = Bits(32) <> OUT.REG init all(0)
  val ack = Bit <> OUT.REG init 0

  val mem = Bits(32) X words <> VAR.REG initFile memfile

  val we = wb_we && wb_cyc
  val addr = wb_adr.uint
  if (wb_rst) ack.din := 0
  else ack.din := wb_cyc && !ack
  if (we && wb_sel(0)) mem(addr)(7, 0).din := wb_dat(7, 0)
  if (we && wb_sel(1)) mem(addr)(15, 8).din := wb_dat(15, 8)
  if (we && wb_sel(2)) mem(addr)(23, 16).din := wb_dat(23, 16)
  if (we && wb_sel(3)) mem(addr)(31, 24).din := wb_dat(31, 24)
  rdt.din := mem(addr)
end servant_ram
