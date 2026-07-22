// DFHDL port of serv_state.v from SERV by Olof Kindgren (see serv_top.scala for the
// port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2019 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_state.v: instruction-phase state, the 0-31 bit counter, and bus/RF handshakes. Clocked on
  * `i_clk` with the synchronous MINI reset `i_rst`. The reset is also read combinationally
  * (`o_ibus_cyc = ibus_cyc & !i_rst`), so it is declared as an explicit `Rst` port; `ibus_cyc_r`
  * powers up at 1 so the reset drives the first instruction fetch as in the original. The MDU ports
  * (`i_mdu_op`, `o_mdu_valid`, `i_mdu_ready`) are omitted (no MDU).
  */
@hw.constraints.timing.clock(portName = "i_clk")
class serv_state extends RTDesign:
  val i_rst = Rst <> IN
  // State
  val i_new_irq = Bit <> IN
  val i_alu_cmp = Bit <> IN
  val o_init = Bit <> OUT
  val o_cnt_en = Bit <> OUT
  val o_cnt0to3 = Bit <> OUT
  val o_cnt12to31 = Bit <> OUT
  val o_cnt0 = Bit <> OUT
  val o_cnt1 = Bit <> OUT
  val o_cnt2 = Bit <> OUT
  val o_cnt3 = Bit <> OUT
  val o_cnt7 = Bit <> OUT
  val o_cnt11 = Bit <> OUT
  val o_cnt12 = Bit <> OUT
  val o_cnt_done = Bit <> OUT
  val o_bufreg_en = Bit <> OUT
  val o_ctrl_pc_en = Bit <> OUT
  val o_ctrl_jump = Bit <> OUT.REG init 0
  val o_ctrl_trap = Bit <> OUT
  val i_ctrl_misalign = Bit <> IN
  val i_sh_done = Bit <> IN
  val o_mem_bytecnt = Bits(2) <> OUT
  val i_mem_misalign = Bit <> IN
  // Control
  val i_bne_or_bge = Bit <> IN
  val i_cond_branch = Bit <> IN
  val i_dbus_en = Bit <> IN
  val i_two_stage_op = Bit <> IN
  val i_branch_op = Bit <> IN
  val i_shift_op = Bit <> IN
  val i_sh_right = Bit <> IN
  val i_alu_rd_sel1 = Bit <> IN
  val i_rd_alu_en = Bit <> IN
  val i_e_op = Bit <> IN
  val i_rd_op = Bit <> IN
  // External
  val o_dbus_cyc = Bit <> OUT
  val i_dbus_ack = Bit <> IN
  val o_ibus_cyc = Bit <> OUT
  val i_ibus_ack = Bit <> IN
  // RF interface
  val o_rf_rreq = Bit <> OUT
  val o_rf_wreq = Bit <> OUT
  val i_rf_ready = Bit <> IN
  val o_rf_rd_en = Bit <> OUT

  val init_done = Bit <> VAR.REG init 0
  val ibus_cyc_r = Bit <> VAR.REG init 1
  val misalign_trap_sync_r = Bit <> VAR.REG init 0
  // the 0-31 bit counter: `cnt` is o_cnt[4:2], `cnt_lsb` is the 4-bit one-hot ring for the LSBs
  val cnt = UInt(3) <> VAR.REG init 0
  val cnt_lsb = Bits(4) <> VAR.REG init all(0)

  o_cnt_en := cnt_lsb.|
  o_cnt_done := (cnt == 7) && cnt_lsb(3)
  o_init := i_two_stage_op && !i_new_irq && !init_done
  o_ctrl_trap := i_e_op || i_new_irq || misalign_trap_sync_r
  o_ctrl_pc_en := o_cnt_en && !o_init

  o_mem_bytecnt := cnt.bits(2, 1)
  o_cnt0to3 := cnt == 0
  o_cnt12to31 := cnt.bits(2) || (cnt.bits(1, 0) == b"11")
  o_cnt0 := (cnt == 0) && cnt_lsb(0)
  o_cnt1 := (cnt == 0) && cnt_lsb(1)
  o_cnt2 := (cnt == 0) && cnt_lsb(2)
  o_cnt3 := (cnt == 0) && cnt_lsb(3)
  o_cnt7 := (cnt == 1) && cnt_lsb(3)
  o_cnt11 := (cnt == 2) && cnt_lsb(3)
  o_cnt12 := (cnt == 3) && cnt_lsb(0)

  val take_branch = i_branch_op && (!i_cond_branch || (i_alu_cmp ^ i_bne_or_bge))
  val last_init = o_cnt_done && o_init
  // only guaranteed to be correct during the last cycle of the init stage
  val trap_pending = (take_branch && i_ctrl_misalign) || (i_dbus_en && i_mem_misalign)

  o_rf_wreq :=
    (i_shift_op && i_sh_right.sel(
      i_sh_done && (last_init || (!o_cnt_en && init_done)),
      last_init
    )) || i_dbus_ack || (i_branch_op && last_init && !trap_pending) ||
      (i_rd_alu_en && i_alu_rd_sel1 && last_init)
  o_dbus_cyc := !o_cnt_en && init_done && i_dbus_en && !i_mem_misalign
  o_rf_rreq := i_ibus_ack || (trap_pending && last_init)
  o_rf_rd_en := i_rd_op && !o_init
  o_bufreg_en := (o_cnt_en && (o_init || ((o_ctrl_trap || i_branch_op) && i_two_stage_op))) ||
    (i_shift_op && init_done && (i_sh_right || i_sh_done))
  o_ibus_cyc := ibus_cyc_r && !i_rst.actual

  if (i_ibus_ack || o_cnt_done) ibus_cyc_r.din := o_ctrl_pc_en
  if (o_cnt_done)
    init_done.din := o_init && !init_done
    o_ctrl_jump.din := o_init && take_branch

  cnt.din := cnt + cnt_lsb(3).toUInt(3)
  cnt_lsb.din := (cnt_lsb(2, 0), (cnt_lsb(3) && !o_cnt_done) || i_rf_ready).toBits

  if (i_ibus_ack || o_cnt_done)
    misalign_trap_sync_r.din :=
      !i_ibus_ack && ((trap_pending && o_init) || misalign_trap_sync_r)

  // The MINI reset targets exactly these four registers. A declared `Rst` port is readable but
  // (unlike a `@timing.reset` annotation) does not auto-reset the registers, so the reset is
  // applied explicitly here to match the baseline's `always @(posedge i_clk) if (i_rst) ...`.
  if (i_rst.actual)
    init_done.din := 0
    o_ctrl_jump.din := 0
    cnt.din := 0
    cnt_lsb.din := all(0)
end serv_state
