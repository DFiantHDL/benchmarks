// DFHDL port of servant_timer.v from the Servant SoC by Olof Kindgren (see serv_top.scala
// for the port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2019 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** servant_timer.v: free-running 32-bit mtime with an mtimecmp compare interrupt (WIDTH = 32,
  * DIVIDER = 0). Clocked on `i_clk` with the synchronous MINI reset `i_rst`. The compare is the
  * original's signed `mtime - mtimecmp >= 0`, expressed here as the inverted sign bit of the
  * wrapping difference.
  */
@hw.constraints.timing.clock(portName = "i_clk")
@hw.constraints.timing.reset(portName = "i_rst")
class servant_timer extends RTDesign:
  val o_irq = Bit <> OUT.REG init 0
  val i_wb_dat = Bits(32) <> IN
  val i_wb_we = Bit <> IN
  val i_wb_cyc = Bit <> IN
  val o_wb_dat = Bits(32) <> OUT

  val mtime = UInt(32) <> VAR.REG init 0
  val mtimecmp = UInt(32) <> VAR.REG init 0

  o_wb_dat := mtime.bits
  if (i_wb_cyc && i_wb_we) mtimecmp.din := i_wb_dat.uint
  mtime.din := mtime + 1
  o_irq.din := !(mtime - mtimecmp).bits(31)
end servant_timer
