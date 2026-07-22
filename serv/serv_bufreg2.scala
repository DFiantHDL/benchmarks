// DFHDL port of serv_bufreg2.v from SERV by Olof Kindgren (see serv_top.scala for the
// port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2022 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_bufreg2.v: buffer register for load/store data and the shift-amount downcounter (W = 1).
  * Clocked on `i_clk`, no reset.
  */
@hw.constraints.timing.clock(portName = "i_clk")
class serv_bufreg2 extends RTDesign:
  // State
  val i_en = Bit <> IN
  val i_init = Bit <> IN
  val i_cnt7 = Bit <> IN
  val i_cnt_done = Bit <> IN
  val i_sh_right = Bit <> IN
  val i_lsb = Bits(2) <> IN
  val i_bytecnt = Bits(2) <> IN
  val o_sh_done = Bit <> OUT
  // Control
  val i_op_b_sel = Bit <> IN
  val i_shift_op = Bit <> IN
  // Data
  val i_rs2 = Bit <> IN
  val i_imm = Bit <> IN
  val o_op_b = Bit <> OUT
  val o_q = Bit <> OUT
  // External
  val o_dat = Bits(32) <> OUT
  val i_load = Bit <> IN
  val i_dat = Bits(32) <> IN

  val dhi = Bits(8) <> VAR.REG init all(0)
  val dlo = Bits(24) <> VAR.REG init all(0)

  // shift the store data into place when i_lsb + i_bytecnt < 4 (expanded expression)
  val byte_valid = (!i_lsb(0) && !i_lsb(1)) || (!i_bytecnt(0) && !i_bytecnt(1)) ||
    (!i_bytecnt(1) && !i_lsb(1)) || (!i_bytecnt(1) && !i_lsb(0)) || (!i_bytecnt(0) && !i_lsb(1))

  o_op_b := i_op_b_sel.sel(i_rs2, i_imm)
  val shift_en = i_shift_op.sel(i_en && i_init && (i_bytecnt == b"00"), i_en && byte_valid)
  val cnt_en = i_shift_op && (!i_init || (i_cnt_done && i_sh_right))
  val cnt_next = (o_op_b, dhi(7), (dhi(5, 0).uint - 1).bits).toBits
  val dat_shamt = cnt_en.sel(cnt_next, (o_op_b, dhi(7, 1)).toBits)
  o_sh_done := dat_shamt(5)
  o_dat := (dhi, dlo).toBits
  o_q :=
    ((i_lsb == b"11") && o_dat(24)) ||
      ((i_lsb == b"10") && o_dat(16)) ||
      ((i_lsb == b"01") && o_dat(8)) ||
      ((i_lsb == b"00") && o_dat(0))
  if (shift_en || cnt_en || i_load)
    dhi.din := i_load.sel(
      i_dat(31, 24),
      dat_shamt & (b"11", !(i_shift_op && i_cnt7 && !cnt_en), b"11111").toBits
    )
  if (shift_en || i_load)
    dlo.din := i_load.sel(i_dat(23, 0), (dhi(0), dlo(23, 1)).toBits)
end serv_bufreg2
