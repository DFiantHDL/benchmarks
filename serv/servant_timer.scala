// DFHDL port of servant_timer.v from the Servant SoC by Olof Kindgren (see serv_top.scala
// for the port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2019 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** servant_timer.v: free-running 32-bit mtime with an mtimecmp compare interrupt (WIDTH = 32,
  * DIVIDER = 0). The compare is the original's signed `mtime - mtimecmp >= 0`, expressed here as
  * the inverted sign bit of the wrapping difference.
  */
class servant_timer extends RTDesign:
  val wb_rst = Bit <> IN
  val irq = Bit <> OUT.REG init 0
  val wb_dat = Bits(32) <> IN
  val wb_we = Bit <> IN
  val wb_cyc = Bit <> IN
  val wb_rdt = Bits(32) <> OUT

  val mtime = UInt(32) <> VAR.REG init 0
  val mtimecmp = UInt(32) <> VAR.REG init 0

  wb_rdt := mtime.bits
  if (wb_cyc && wb_we) mtimecmp.din := wb_dat.uint
  mtime.din := mtime + 1
  irq.din := !(mtime - mtimecmp).bits(31)
  if (wb_rst)
    mtime.din := 0
    mtimecmp.din := 0
end servant_timer
