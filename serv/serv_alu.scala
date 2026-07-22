// DFHDL port of serv_alu.v from SERV by Olof Kindgren (see serv_top.scala for the
// port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2018 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_alu.v: the bit-serial ALU (W = 1). Clocked on `clk`, no reset. */
@hw.constraints.timing.clock(portName = "clk")
class serv_alu extends RTDesign:
  // State
  val i_en = Bit <> IN
  val i_cnt0 = Bit <> IN
  val o_cmp = Bit <> OUT
  // Control
  val i_sub = Bit <> IN
  val i_bool_op = Bits(2) <> IN
  val i_cmp_eq = Bit <> IN
  val i_cmp_sig = Bit <> IN
  val i_rd_sel = Bits(3) <> IN
  // Data
  val i_rs1 = Bit <> IN
  val i_op_b = Bit <> IN
  val i_buf = Bit <> IN
  val o_rd = Bit <> OUT

  val cmp_r = Bit <> VAR.REG init 0
  val add_cy_r = Bit <> VAR.REG init 0

  // sign-extended operands (only the MSB cycle matters, gated by i_cmp_sig)
  val rs1_sx = i_rs1 && i_cmp_sig
  val op_b_sx = i_op_b && i_cmp_sig
  val add_b = i_op_b ^ i_sub
  val result_add = i_rs1 ^ add_b ^ add_cy_r
  val add_cy = (i_rs1 && add_b) || (i_rs1 && add_cy_r) || (add_b && add_cy_r)
  val result_lt = rs1_sx ^ !op_b_sx ^ add_cy
  val result_eq = !result_add && (cmp_r || i_cnt0)
  o_cmp := i_cmp_eq.sel(result_eq, result_lt)
  // 00 xor, 01 zero (shift ops), 10 or, 11 and
  val result_bool = ((i_rs1 ^ i_op_b) && !i_bool_op(0)) || (i_bool_op(1) && i_op_b && i_rs1)
  val result_slt = cmp_r && i_cnt0
  o_rd := i_buf || (i_rd_sel(0) && result_add) || (i_rd_sel(1) && result_slt) ||
    (i_rd_sel(2) && result_bool)
  add_cy_r.din := i_en.sel(add_cy, i_sub)
  if (i_en) cmp_r.din := o_cmp
end serv_alu
