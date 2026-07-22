// DFHDL port of serv_state.v from SERV by Olof Kindgren (see serv_top.scala for the
// port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2019 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_state.v: instruction-phase state, the 0-31 bit counter, and bus/RF handshakes. */
class serv_state extends RTDesign:
  val wb_rst = Bit <> IN
  // State
  val new_irq = Bit <> IN
  val alu_cmp = Bit <> IN
  val ctrl_misalign = Bit <> IN
  val sh_done = Bit <> IN
  val mem_misalign = Bit <> IN
  // Control
  val bne_or_bge = Bit <> IN
  val cond_branch = Bit <> IN
  val dbus_en = Bit <> IN
  val two_stage_op = Bit <> IN
  val branch_op = Bit <> IN
  val shift_op = Bit <> IN
  val sh_right = Bit <> IN
  val alu_rd_sel1 = Bit <> IN
  val rd_alu_en = Bit <> IN
  val e_op = Bit <> IN
  val rd_op = Bit <> IN
  // External
  val dbus_ack = Bit <> IN
  val ibus_ack = Bit <> IN
  val rf_ready = Bit <> IN
  // Outputs
  val init_stage = Bit <> OUT
  val cnt_en = Bit <> OUT
  val cnt0to3 = Bit <> OUT
  val cnt12to31 = Bit <> OUT
  val cnt0 = Bit <> OUT
  val cnt1 = Bit <> OUT
  val cnt2 = Bit <> OUT
  val cnt3 = Bit <> OUT
  val cnt7 = Bit <> OUT
  val cnt11 = Bit <> OUT
  val cnt12 = Bit <> OUT
  val cnt_done = Bit <> OUT
  val bufreg_en = Bit <> OUT
  val ctrl_pc_en = Bit <> OUT
  val ctrl_jump = Bit <> OUT.REG init 0
  val ctrl_trap = Bit <> OUT
  val mem_bytecnt = Bits(2) <> OUT
  val dbus_cyc = Bit <> OUT
  val ibus_cyc = Bit <> OUT
  val rf_rreq = Bit <> OUT
  val rf_wreq = Bit <> OUT
  val rf_rd_en = Bit <> OUT

  val init_done = Bit <> VAR.REG init 0
  val ibus_cyc_r = Bit <> VAR.REG init 0
  val misalign_trap_sync_r = Bit <> VAR.REG init 0
  // the 0-31 bit counter: `cnt` is o_cnt[4:2], `cnt_lsb` is the 4-bit one-hot ring for the LSBs
  val cnt = UInt(3) <> VAR.REG init 0
  val cnt_lsb = Bits(4) <> VAR.REG init all(0)

  cnt_en := cnt_lsb.|
  cnt_done := (cnt == 7) && cnt_lsb(3)
  init_stage := two_stage_op && !new_irq && !init_done
  ctrl_trap := e_op || new_irq || misalign_trap_sync_r
  ctrl_pc_en := cnt_en && !init_stage

  mem_bytecnt := cnt.bits(2, 1)
  cnt0to3 := cnt == 0
  cnt12to31 := cnt.bits(2) || (cnt.bits(1, 0) == b"11")
  cnt0 := (cnt == 0) && cnt_lsb(0)
  cnt1 := (cnt == 0) && cnt_lsb(1)
  cnt2 := (cnt == 0) && cnt_lsb(2)
  cnt3 := (cnt == 0) && cnt_lsb(3)
  cnt7 := (cnt == 1) && cnt_lsb(3)
  cnt11 := (cnt == 2) && cnt_lsb(3)
  cnt12 := (cnt == 3) && cnt_lsb(0)

  val take_branch = branch_op && (!cond_branch || (alu_cmp ^ bne_or_bge))
  val last_init = cnt_done && init_stage
  // only guaranteed to be correct during the last cycle of the init stage
  val trap_pending = (take_branch && ctrl_misalign) || (dbus_en && mem_misalign)

  rf_wreq :=
    (shift_op && sh_right.sel(
      sh_done && (last_init || (!cnt_en && init_done)),
      last_init
    )) || dbus_ack || (branch_op && last_init && !trap_pending) ||
      (rd_alu_en && alu_rd_sel1 && last_init)
  dbus_cyc := !cnt_en && init_done && dbus_en && !mem_misalign
  rf_rreq := ibus_ack || (trap_pending && last_init)
  rf_rd_en := rd_op && !init_stage
  bufreg_en := (cnt_en && (init_stage || ((ctrl_trap || branch_op) && two_stage_op))) ||
    (shift_op && init_done && (sh_right || sh_done))
  ibus_cyc := ibus_cyc_r && !wb_rst

  if (ibus_ack || cnt_done || wb_rst) ibus_cyc_r.din := ctrl_pc_en || wb_rst
  if (cnt_done)
    init_done.din := init_stage && !init_done
    ctrl_jump.din := init_stage && take_branch
  if (wb_rst)
    init_done.din := 0
    ctrl_jump.din := 0

  cnt.din := cnt + cnt_lsb(3).toUInt(3)
  cnt_lsb.din := (cnt_lsb(2, 0), (cnt_lsb(3) && !cnt_done) || rf_ready).toBits
  if (wb_rst)
    cnt.din := 0
    cnt_lsb.din := all(0)

  if (ibus_ack || cnt_done || wb_rst)
    misalign_trap_sync_r.din :=
      !(ibus_ack || wb_rst) && ((trap_pending && init_stage) || misalign_trap_sync_r)
end serv_state
