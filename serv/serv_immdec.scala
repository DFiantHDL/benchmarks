// DFHDL port of serv_immdec.v from SERV by Olof Kindgren (see serv_top.scala for the
// port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2020 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_immdec.v: serial immediate decoder (W = 1, shared RF-address/imm registers). */
class serv_immdec extends RTDesign:
  val cnt_en = Bit <> IN
  val cnt_done = Bit <> IN
  val immdec_en = Bits(4) <> IN
  val csr_imm_en = Bit <> IN
  val ctrl = Bits(4) <> IN
  val wb_en = Bit <> IN
  val wb_rdt = Bits(32) <> IN // instruction word (bits 31:7 are used)
  val rd_addr = Bits(5) <> OUT
  val rs1_addr = Bits(5) <> OUT
  val rs2_addr = Bits(5) <> OUT
  val csr_imm = Bit <> OUT
  val imm = Bit <> OUT

  val imm31 = Bit <> VAR.REG init 0
  val imm19_12_20 = Bits(9) <> VAR.REG init all(0)
  val imm7 = Bit <> VAR.REG init 0
  val imm30_25 = Bits(6) <> VAR.REG init all(0)
  val imm24_20 = Bits(5) <> VAR.REG init all(0)
  val imm11_7 = Bits(5) <> VAR.REG init all(0)

  csr_imm := imm19_12_20(4)
  // CSR immediates are always zero-extended, hence clear the signbit
  val signbit = imm31 && !csr_imm_en
  rs1_addr := imm19_12_20(8, 4)
  rs2_addr := imm24_20
  rd_addr := imm11_7

  if (wb_en) imm31.din := wb_rdt(31)
  if (wb_en) imm19_12_20.din := (wb_rdt(19, 12), wb_rdt(20)).toBits
  else if (cnt_en && immdec_en(1))
    imm19_12_20.din := (ctrl(3).sel(signbit, imm24_20(0)), imm19_12_20(8, 1)).toBits
  if (wb_en) imm7.din := wb_rdt(7)
  else if (cnt_en) imm7.din := signbit
  if (wb_en) imm30_25.din := wb_rdt(30, 25)
  else if (cnt_en && immdec_en(3))
    imm30_25.din :=
      (ctrl(2).sel(imm7, ctrl(1).sel(signbit, imm19_12_20(0))), imm30_25(5, 1)).toBits
  if (wb_en) imm24_20.din := wb_rdt(24, 20)
  else if (cnt_en && immdec_en(2)) imm24_20.din := (imm30_25(0), imm24_20(4, 1)).toBits
  if (wb_en) imm11_7.din := wb_rdt(11, 7)
  else if (cnt_en && immdec_en(0)) imm11_7.din := (imm30_25(0), imm11_7(4, 1)).toBits

  imm := cnt_done.sel(signbit, ctrl(0).sel(imm11_7(0), imm24_20(0)))
end serv_immdec
