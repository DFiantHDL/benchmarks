// DFHDL port of serv_csr.v from SERV by Olof Kindgren (see serv_top.scala for the
// port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2018 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_csr.v: the in-core CSRs (mstatus/mie/mcause) and timer-interrupt logic (W = 1). */
class serv_csr extends RTDesign:
  val wb_rst = Bit <> IN
  // State
  val trig_irq = Bit <> IN // ibus_ack
  val en = Bit <> IN // cnt_en
  val cnt0to3 = Bit <> IN
  val cnt3 = Bit <> IN
  val cnt7 = Bit <> IN
  val cnt11 = Bit <> IN
  val cnt12 = Bit <> IN
  val cnt_done = Bit <> IN
  val mem_op = Bit <> IN // !mtval_pc
  val mtip = Bit <> IN
  val trap = Bit <> IN
  // Control
  val e_op = Bit <> IN
  val ebreak = Bit <> IN
  val mem_cmd = Bit <> IN
  val mstatus_en = Bit <> IN
  val mie_en = Bit <> IN
  val mcause_en = Bit <> IN
  val csr_source = Bits(2) <> IN // 00 CSR, 01 EXT, 10 SET, 11 CLR
  val mret = Bit <> IN
  val csr_d_sel = Bit <> IN
  // Data
  val rf_csr_out = Bit <> IN
  val csr_imm = Bit <> IN
  val rs1 = Bit <> IN
  val new_irq = Bit <> OUT.REG init 0
  val csr_in = Bit <> OUT
  val rd = Bit <> OUT // o_q

  val mstatus_mie = Bit <> VAR.REG init 0
  val mstatus_mpie = Bit <> VAR.REG init 0
  val mie_mtie = Bit <> VAR.REG init 0
  val mcause31 = Bit <> VAR.REG init 0
  val mcause3_0 = Bits(4) <> VAR.REG init all(0)
  val timer_irq_r = Bit <> VAR.REG init 0

  val d = csr_d_sel.sel(csr_imm, rs1)
  val mstatus = (mstatus_mie && cnt3) || cnt11 || cnt12
  val mcause = cnt0to3.sel(mcause3_0(0), cnt_done && mcause31)
  val csr_out = (mstatus_en && en && mstatus) || rf_csr_out || (mcause_en && en && mcause)
  rd := csr_out

  val csr_in_w = Bit <> VAR
  if (csr_source == b"01") csr_in_w := d
  else if (csr_source == b"10") csr_in_w := csr_out || d
  else if (csr_source == b"11") csr_in_w := csr_out && !d
  else csr_in_w := csr_out
  csr_in := csr_in_w

  val timer_irq = mtip && mstatus_mie && mie_mtie

  if (trig_irq)
    timer_irq_r.din := timer_irq
    new_irq.din := timer_irq && !timer_irq_r
  if (mie_en && cnt7) mie_mtie.din := csr_in_w
  // mie: cleared on trap, restored from mpie on mret, written on mstatus bit-3 access
  if ((trap && cnt_done) || (mstatus_en && cnt3 && en) || mret)
    mstatus_mie.din := !trap && mret.sel(mstatus_mpie, csr_in_w)
  // mpie (mstatus bit 7) is not readable or writable from software
  if (trap && cnt_done) mstatus_mpie.din := mstatus_mie
  // exception code: timer=7, ebreak=3, ecall=11, misaligned load=4/store=6/jump=0
  if ((mcause_en && en && cnt0to3) || (trap && cnt_done))
    mcause3_0(3).din := (e_op && !ebreak) || (!trap && csr_in_w)
    mcause3_0(2).din := new_irq || mem_op || (!trap && mcause3_0(3))
    mcause3_0(1).din := new_irq || e_op || (mem_op && mem_cmd) || (!trap && mcause3_0(2))
    mcause3_0(0).din := new_irq || e_op || (!trap && mcause3_0(1))
  if ((mcause_en && cnt_done) || trap) mcause31.din := trap.sel(new_irq, csr_in_w)
  if (wb_rst)
    new_irq.din := 0
    mie_mtie.din := 0
end serv_csr
