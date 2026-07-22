// DFHDL port of serv_rf_ram_if.v from SERV by Olof Kindgren (see serv_top.scala for the
// port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2019 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_rf_ram_if.v: serial-to-SRAM shift interface between SERV and the RF storage (width = 2, W =
  * 1, ratio = 2). Clocked on `i_clk` with the synchronous MINI reset `i_rst`.
  */
@hw.constraints.timing.clock(portName = "i_clk")
@hw.constraints.timing.reset(portName = "i_rst")
class serv_rf_ram_if extends RTDesign:
  // SERV side
  val i_wreq = Bit <> IN
  val i_rreq = Bit <> IN
  val o_ready = Bit <> OUT
  val i_wreg0 = Bits(6) <> IN
  val i_wreg1 = Bits(6) <> IN
  val i_wen0 = Bit <> IN
  val i_wen1 = Bit <> IN
  val i_wdata0 = Bit <> IN
  val i_wdata1 = Bit <> IN
  val i_rreg0 = Bits(6) <> IN
  val i_rreg1 = Bits(6) <> IN
  val o_rdata0 = Bit <> OUT
  val o_rdata1 = Bit <> OUT
  // RAM side
  val o_waddr = Bits(10) <> OUT
  val o_wdata = Bits(2) <> OUT
  val o_wen = Bit <> OUT
  val o_raddr = Bits(10) <> OUT
  val o_ren = Bit <> OUT
  val i_rdata = Bits(2) <> IN

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

  o_ready := rgnt || i_wreq
  val wcnt = rcnt - 4
  val wtrig0 = rtrig1
  val wtrig1 = wcnt.bits(0)
  o_wdata := wtrig1.sel(wdata1_r(1, 0), wdata0_r)
  val wreg = wtrig1.sel(i_wreg1, i_wreg0)
  o_waddr := (wreg, wcnt.bits(4, 1)).toBits
  o_wen := (wtrig0 && wen0_r) || (wtrig1 && wen1_r)

  val rtrig0 = rcnt.bits(0)
  val rreg = rtrig0.sel(i_rreg1, i_rreg0)
  o_raddr := (rreg, rcnt.bits(4, 1)).toBits
  o_rdata0 := rdata0_r(0)
  o_rdata1 := rtrig1.sel(i_rdata(0), rdata1_r)
  o_ren := rgate

  if (wcnt.bits(0))
    wen0_r.din := i_wen0
    wen1_r.din := i_wen1
  wdata0_r.din := (i_wdata0, wdata0_r(1)).toBits
  wdata1_r.din := (i_wdata1, wdata1_r(2, 1)).toBits

  if (rtrig1) rdata1_r.din := i_rdata(1)
  if ((rcnt.bits.&) || i_rreq) rgate.din := i_rreq
  rtrig1.din := rtrig0
  rcnt.din := rcnt + 1
  if (i_rreq || i_wreq) rcnt.din := i_wreq.sel(d"5'2", d"5'0")
  rreq_r.din := i_rreq
  rgnt.din := rreq_r
  rdata0_r.din := (0, rdata0_r(1)).toBits
  if (rtrig0) rdata0_r.din := i_rdata
end serv_rf_ram_if
