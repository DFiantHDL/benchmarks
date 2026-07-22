// DFHDL port of serv_alu.v from SERV by Olof Kindgren (see serv_top.scala for the
// port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2018 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_alu.v: the bit-serial ALU (W = 1). */
class serv_alu extends RTDesign:
  val en = Bit <> IN // cnt_en
  val cnt0 = Bit <> IN
  val sub = Bit <> IN
  val bool_op = Bits(2) <> IN
  val cmp_eq = Bit <> IN
  val cmp_sig = Bit <> IN
  val rd_sel = Bits(3) <> IN
  val rs1 = Bit <> IN
  val op_b = Bit <> IN
  val bufreg_q = Bit <> IN
  val cmp = Bit <> OUT
  val rd = Bit <> OUT

  val cmp_r = Bit <> VAR.REG init 0
  val add_cy_r = Bit <> VAR.REG init 0

  // sign-extended operands (only the MSB cycle matters, gated by cmp_sig)
  val rs1_sx = rs1 && cmp_sig
  val op_b_sx = op_b && cmp_sig
  val add_b = op_b ^ sub
  val result_add = rs1 ^ add_b ^ add_cy_r
  val add_cy = (rs1 && add_b) || (rs1 && add_cy_r) || (add_b && add_cy_r)
  val result_lt = rs1_sx ^ !op_b_sx ^ add_cy
  val result_eq = !result_add && (cmp_r || cnt0)
  cmp := cmp_eq.sel(result_eq, result_lt)
  // 00 xor, 01 zero (shift ops), 10 or, 11 and
  val result_bool = ((rs1 ^ op_b) && !bool_op(0)) || (bool_op(1) && op_b && rs1)
  val result_slt = cmp_r && cnt0
  rd := bufreg_q || (rd_sel(0) && result_add) || (rd_sel(1) && result_slt) ||
    (rd_sel(2) && result_bool)
  add_cy_r.din := en.sel(add_cy, sub)
  if (en) cmp_r.din := cmp
end serv_alu
