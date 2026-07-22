// DFHDL port of serv_rf_ram_if.v from SERV by Olof Kindgren (see serv_top.scala for the
// port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2019 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_rf_ram_if.v: serial-to-SRAM shift interface between SERV and the RF storage (width = 2, W =
  * 1, ratio = 2).
  */
class serv_rf_ram_if extends RTDesign:
  val wb_rst = Bit <> IN
  // SERV side
  val wreq = Bit <> IN
  val rreq = Bit <> IN
  val ready = Bit <> OUT
  val wreg0 = Bits(6) <> IN
  val wreg1 = Bits(6) <> IN
  val wen0 = Bit <> IN
  val wen1 = Bit <> IN
  val wdata0 = Bit <> IN
  val wdata1 = Bit <> IN
  val rreg0 = Bits(6) <> IN
  val rreg1 = Bits(6) <> IN
  val rdata0 = Bit <> OUT
  val rdata1 = Bit <> OUT
  // RAM side
  val waddr = Bits(10) <> OUT
  val wdata = Bits(2) <> OUT
  val wen = Bit <> OUT
  val raddr = Bits(10) <> OUT
  val ren = Bit <> OUT
  val ram_rdata = Bits(2) <> IN

  val rgnt = Bit <> VAR.REG init 0
  val rcnt = UInt(5) <> VAR.REG init 0
  val rtrig1 = Bit <> VAR.REG init 0
  // write side
  val wdata0_r = Bits(2) <> VAR.REG init all(0)
  val wdata1_r = Bits(3) <> VAR.REG init all(0)
  val wen0_r = Bit <> VAR.REG init 0
  val wen1_r = Bit <> VAR.REG init 0
  // read side
  val rdata0_r = Bits(2) <> VAR.REG init all(0)
  val rdata1_r = Bit <> VAR.REG init 0
  val rgate = Bit <> VAR.REG init 0
  val rreq_r = Bit <> VAR.REG init 0

  ready := rgnt || wreq
  val wcnt = rcnt - 4
  val wtrig0 = rtrig1
  val wtrig1 = wcnt.bits(0)
  wdata := wtrig1.sel(wdata1_r(1, 0), wdata0_r)
  val wreg = wtrig1.sel(wreg1, wreg0)
  waddr := (wreg, wcnt.bits(4, 1)).toBits
  wen := (wtrig0 && wen0_r) || (wtrig1 && wen1_r)

  val rtrig0 = rcnt.bits(0)
  val rreg = rtrig0.sel(rreg1, rreg0)
  raddr := (rreg, rcnt.bits(4, 1)).toBits
  rdata0 := rdata0_r(0)
  rdata1 := rtrig1.sel(ram_rdata(0), rdata1_r)
  ren := rgate

  if (wcnt.bits(0))
    wen0_r.din := wen0
    wen1_r.din := wen1
  wdata0_r.din := (wdata0, wdata0_r(1)).toBits
  wdata1_r.din := (wdata1, wdata1_r(2, 1)).toBits

  if (rtrig1) rdata1_r.din := ram_rdata(1)
  if ((rcnt.bits.&) || rreq) rgate.din := rreq
  rtrig1.din := rtrig0
  rcnt.din := rcnt + 1
  if (rreq || wreq) rcnt.din := wreq.sel(d"5'2", d"5'0")
  rreq_r.din := rreq
  rgnt.din := rreq_r
  rdata0_r.din := (0, rdata0_r(1)).toBits
  if (rtrig0) rdata0_r.din := ram_rdata
  if (wb_rst)
    rgate.din := 0
    rgnt.din := 0
    rreq_r.din := 0
    rcnt.din := 0
end serv_rf_ram_if
