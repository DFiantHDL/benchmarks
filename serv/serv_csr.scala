// DFHDL port of serv_csr.v from SERV by Olof Kindgren (see serv_top.scala for the
// port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2018 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_csr.v: the in-core CSRs (mstatus/mie/mcause) and timer-interrupt logic (W = 1). Clocked on
  * `i_clk` with the synchronous MINI reset `i_rst` (resets o_new_irq and mie_mtie; the remaining
  * registers also power up at 0 for cross-simulator determinism).
  */
@hw.constraints.timing.clock(portName = "i_clk")
@hw.constraints.timing.reset(portName = "i_rst")
class serv_csr extends RTDesign:
  // State
  val i_trig_irq = Bit <> IN // ibus_ack
  val i_en = Bit <> IN // cnt_en
  val i_cnt0to3 = Bit <> IN
  val i_cnt3 = Bit <> IN
  val i_cnt7 = Bit <> IN
  val i_cnt11 = Bit <> IN
  val i_cnt12 = Bit <> IN
  val i_cnt_done = Bit <> IN
  val i_mem_op = Bit <> IN // !mtval_pc
  val i_mtip = Bit <> IN
  val i_trap = Bit <> IN
  val o_new_irq = Bit <> OUT.REG init 0
  // Control
  val i_e_op = Bit <> IN
  val i_ebreak = Bit <> IN
  val i_mem_cmd = Bit <> IN
  val i_mstatus_en = Bit <> IN
  val i_mie_en = Bit <> IN
  val i_mcause_en = Bit <> IN
  val i_csr_source = Bits(2) <> IN // 00 CSR, 01 EXT, 10 SET, 11 CLR
  val i_mret = Bit <> IN
  val i_csr_d_sel = Bit <> IN
  // Data
  val i_rf_csr_out = Bit <> IN
  val o_csr_in = Bit <> OUT
  val i_csr_imm = Bit <> IN
  val i_rs1 = Bit <> IN
  val o_q = Bit <> OUT

  val mstatus_mie = Bit <> VAR.REG init 0
  val mstatus_mpie = Bit <> VAR.REG init 0
  val mie_mtie = Bit <> VAR.REG init 0
  val mcause31 = Bit <> VAR.REG init 0
  val mcause3_0 = Bits(4) <> VAR.REG init all(0)
  val timer_irq_r = Bit <> VAR.REG init 0

  val d = i_csr_d_sel.sel(i_csr_imm, i_rs1)
  val mstatus = (mstatus_mie && i_cnt3) || i_cnt11 || i_cnt12
  val mcause = i_cnt0to3.sel(mcause3_0(0), i_cnt_done && mcause31)
  val csr_out = (i_mstatus_en && i_en && mstatus) || i_rf_csr_out || (i_mcause_en && i_en && mcause)
  o_q := csr_out

  val csr_in_w = Bit <> VAR
  if (i_csr_source == b"01") csr_in_w := d
  else if (i_csr_source == b"10") csr_in_w := csr_out || d
  else if (i_csr_source == b"11") csr_in_w := csr_out && !d
  else csr_in_w := csr_out
  o_csr_in := csr_in_w

  val timer_irq = i_mtip && mstatus_mie && mie_mtie

  if (i_trig_irq)
    timer_irq_r.din := timer_irq
    o_new_irq.din := timer_irq && !timer_irq_r
  if (i_mie_en && i_cnt7) mie_mtie.din := csr_in_w
  // mie: cleared on trap, restored from mpie on mret, written on mstatus bit-3 access
  if ((i_trap && i_cnt_done) || (i_mstatus_en && i_cnt3 && i_en) || i_mret)
    mstatus_mie.din := !i_trap && i_mret.sel(mstatus_mpie, csr_in_w)
  // mpie (mstatus bit 7) is not readable or writable from software
  if (i_trap && i_cnt_done) mstatus_mpie.din := mstatus_mie
  // exception code: timer=7, ebreak=3, ecall=11, misaligned load=4/store=6/jump=0
  if ((i_mcause_en && i_en && i_cnt0to3) || (i_trap && i_cnt_done))
    mcause3_0(3).din := (i_e_op && !i_ebreak) || (!i_trap && csr_in_w)
    mcause3_0(2).din := o_new_irq || i_mem_op || (!i_trap && mcause3_0(3))
    mcause3_0(1).din := o_new_irq || i_e_op || (i_mem_op && i_mem_cmd) || (!i_trap && mcause3_0(2))
    mcause3_0(0).din := o_new_irq || i_e_op || (!i_trap && mcause3_0(1))
  if ((i_mcause_en && i_cnt_done) || i_trap) mcause31.din := i_trap.sel(o_new_irq, csr_in_w)
end serv_csr
