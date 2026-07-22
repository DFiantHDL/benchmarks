// DFHDL port of serv_immdec.v from SERV by Olof Kindgren (see serv_top.scala for the
// port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2020 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_immdec.v: serial immediate decoder (W = 1, shared RF-address/imm registers). Clocked on
  * `i_clk`, no reset.
  */
@hw.constraints.timing.clock(portName = "i_clk")
class serv_immdec extends RTDesign:
  // State
  val i_cnt_en = Bit <> IN
  val i_cnt_done = Bit <> IN
  // Control
  val i_immdec_en = Bits(4) <> IN
  val i_csr_imm_en = Bit <> IN
  val i_ctrl = Bits(4) <> IN
  val o_rd_addr = Bits(5) <> OUT
  val o_rs1_addr = Bits(5) <> OUT
  val o_rs2_addr = Bits(5) <> OUT
  // Data
  val o_csr_imm = Bit <> OUT
  val o_imm = Bit <> OUT
  // External
  val i_wb_en = Bit <> IN
  val i_wb_rdt = Bits(32) <> IN // instruction word (bits 31:7 are used)

  val imm31 = Bit <> VAR.REG init 0
  val imm19_12_20 = Bits(9) <> VAR.REG init all(0)
  val imm7 = Bit <> VAR.REG init 0
  val imm30_25 = Bits(6) <> VAR.REG init all(0)
  val imm24_20 = Bits(5) <> VAR.REG init all(0)
  val imm11_7 = Bits(5) <> VAR.REG init all(0)

  o_csr_imm := imm19_12_20(4)
  // CSR immediates are always zero-extended, hence clear the signbit
  val signbit = imm31 && !i_csr_imm_en
  o_rs1_addr := imm19_12_20(8, 4)
  o_rs2_addr := imm24_20
  o_rd_addr := imm11_7

  if (i_wb_en) imm31.din := i_wb_rdt(31)
  if (i_wb_en) imm19_12_20.din := (i_wb_rdt(19, 12), i_wb_rdt(20)).toBits
  else if (i_cnt_en && i_immdec_en(1))
    imm19_12_20.din := (i_ctrl(3).sel(signbit, imm24_20(0)), imm19_12_20(8, 1)).toBits
  if (i_wb_en) imm7.din := i_wb_rdt(7)
  else if (i_cnt_en) imm7.din := signbit
  if (i_wb_en) imm30_25.din := i_wb_rdt(30, 25)
  else if (i_cnt_en && i_immdec_en(3))
    imm30_25.din :=
      (i_ctrl(2).sel(imm7, i_ctrl(1).sel(signbit, imm19_12_20(0))), imm30_25(5, 1)).toBits
  if (i_wb_en) imm24_20.din := i_wb_rdt(24, 20)
  else if (i_cnt_en && i_immdec_en(2)) imm24_20.din := (imm30_25(0), imm24_20(4, 1)).toBits
  if (i_wb_en) imm11_7.din := i_wb_rdt(11, 7)
  else if (i_cnt_en && i_immdec_en(0)) imm11_7.din := (imm30_25(0), imm11_7(4, 1)).toBits

  o_imm := i_cnt_done.sel(signbit, i_ctrl(0).sel(imm11_7(0), imm24_20(0)))
end serv_immdec
