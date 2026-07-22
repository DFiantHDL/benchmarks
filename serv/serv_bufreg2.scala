// DFHDL port of serv_bufreg2.v from SERV by Olof Kindgren (see serv_top.scala for the
// port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2022 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_bufreg2.v: buffer register for load/store data and the shift-amount downcounter (W = 1).
  */
class serv_bufreg2 extends RTDesign:
  val en = Bit <> IN // cnt_en
  val init_stage = Bit <> IN
  val cnt7 = Bit <> IN
  val cnt_done = Bit <> IN
  val sh_right = Bit <> IN
  val lsb = Bits(2) <> IN
  val bytecnt = Bits(2) <> IN
  val op_b_sel = Bit <> IN
  val shift_op = Bit <> IN
  val rs2 = Bit <> IN
  val imm = Bit <> IN
  val load = Bit <> IN // dbus_ack
  val dat_in = Bits(32) <> IN
  val sh_done = Bit <> OUT
  val op_b = Bit <> OUT
  val q = Bit <> OUT
  val dat = Bits(32) <> OUT

  val dhi = Bits(8) <> VAR.REG init all(0)
  val dlo = Bits(24) <> VAR.REG init all(0)

  // shift the store data into place when i_lsb + i_bytecnt < 4 (expanded expression)
  val byte_valid = (!lsb(0) && !lsb(1)) || (!bytecnt(0) && !bytecnt(1)) ||
    (!bytecnt(1) && !lsb(1)) || (!bytecnt(1) && !lsb(0)) || (!bytecnt(0) && !lsb(1))

  op_b := op_b_sel.sel(rs2, imm)
  val shift_en = shift_op.sel(en && init_stage && (bytecnt == b"00"), en && byte_valid)
  val cnt_en_w = shift_op && (!init_stage || (cnt_done && sh_right))
  val cnt_next = (op_b, dhi(7), (dhi(5, 0).uint - 1).bits).toBits
  val dat_shamt = cnt_en_w.sel(cnt_next, (op_b, dhi(7, 1)).toBits)
  sh_done := dat_shamt(5)
  dat := (dhi, dlo).toBits
  q :=
    ((lsb == b"11") && dat(24)) ||
      ((lsb == b"10") && dat(16)) ||
      ((lsb == b"01") && dat(8)) ||
      ((lsb == b"00") && dat(0))
  if (shift_en || cnt_en_w || load)
    dhi.din := load.sel(
      dat_in(31, 24),
      dat_shamt & (b"11", !(shift_op && cnt7 && !cnt_en_w), b"11111").toBits
    )
  if (shift_en || load)
    dlo.din := load.sel(dat_in(23, 0), (dhi(0), dlo(23, 1)).toBits)
end serv_bufreg2
