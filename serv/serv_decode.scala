// DFHDL port of serv_decode.v from SERV by Olof Kindgren (see serv_top.scala for the
// port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2018 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_decode.v: instruction word to control signals (PRE_REGISTER form: the raw instruction
  * fields are registered on fetch and the control outputs decode combinationally).
  */
class serv_decode extends RTDesign:
  val wb_rdt = Bits(32) <> IN // instruction word (bits 31:2 are used)
  val wb_en = Bit <> IN
  // to state
  val sh_right = Bit <> OUT
  val bne_or_bge = Bit <> OUT
  val cond_branch = Bit <> OUT
  val e_op = Bit <> OUT
  val ebreak = Bit <> OUT
  val branch_op = Bit <> OUT
  val shift_op = Bit <> OUT
  val rd_op = Bit <> OUT
  val two_stage_op = Bit <> OUT
  val dbus_en = Bit <> OUT
  // to bufreg
  val bufreg_rs1_en = Bit <> OUT
  val bufreg_imm_en = Bit <> OUT
  val bufreg_clr_lsb = Bit <> OUT
  val bufreg_sh_signed = Bit <> OUT
  // to ctrl
  val ctrl_jal_or_jalr = Bit <> OUT
  val ctrl_utype = Bit <> OUT
  val ctrl_pc_rel = Bit <> OUT
  val ctrl_mret = Bit <> OUT
  // to alu
  val alu_sub = Bit <> OUT
  val alu_bool_op = Bits(2) <> OUT
  val alu_cmp_eq = Bit <> OUT
  val alu_cmp_sig = Bit <> OUT
  val alu_rd_sel = Bits(3) <> OUT
  // to mem IF
  val mem_signed = Bit <> OUT
  val mem_word = Bit <> OUT
  val mem_half = Bit <> OUT
  val mem_cmd = Bit <> OUT
  // to CSR
  val csr_en = Bit <> OUT
  val csr_addr = Bits(2) <> OUT
  val csr_mstatus_en = Bit <> OUT
  val csr_mie_en = Bit <> OUT
  val csr_mcause_en = Bit <> OUT
  val csr_source = Bits(2) <> OUT
  val csr_d_sel = Bit <> OUT
  val csr_imm_en = Bit <> OUT
  val mtval_pc = Bit <> OUT
  // to top
  val immdec_ctrl = Bits(4) <> OUT
  val immdec_en = Bits(4) <> OUT
  val op_b_source = Bit <> OUT
  // to RF IF
  val rd_mem_en = Bit <> OUT
  val rd_csr_en = Bit <> OUT
  val rd_alu_en = Bit <> OUT

  val opcode = Bits(5) <> VAR.REG init all(0)
  val funct3 = Bits(3) <> VAR.REG init all(0)
  val op20 = Bit <> VAR.REG init 0
  val op21 = Bit <> VAR.REG init 0
  val op22 = Bit <> VAR.REG init 0
  val op26 = Bit <> VAR.REG init 0
  val imm25 = Bit <> VAR.REG init 0
  val imm30 = Bit <> VAR.REG init 0

  if (wb_en)
    funct3.din := wb_rdt(14, 12)
    imm30.din := wb_rdt(30)
    imm25.din := wb_rdt(25)
    opcode.din := wb_rdt(6, 2)
    op20.din := wb_rdt(20)
    op21.din := wb_rdt(21)
    op22.din := wb_rdt(22)
    op26.din := wb_rdt(26)

  two_stage_op := !opcode(2) || (funct3(0) && !funct3(1) && !opcode(0) && !opcode(4)) ||
    (funct3(1) && !funct3(2) && !opcode(0) && !opcode(4))
  shift_op := opcode(2) && !funct3(1)
  branch_op := opcode(4)
  dbus_en := !opcode(2) && !opcode(4)
  mtval_pc := opcode(4)
  mem_word := funct3(1)
  rd_alu_en := !opcode(0) && opcode(2) && !opcode(4)
  rd_mem_en := !opcode(2) && !opcode(0)

  // jal,branch = imm; jalr = rs1+imm; mem = rs1+imm; shift = rs1
  bufreg_rs1_en := !opcode(4) || (!opcode(1) && opcode(0))
  bufreg_imm_en := !opcode(2)
  // clear LSB of the immediate for BRANCH and JAL ops
  bufreg_clr_lsb := opcode(4) && ((opcode(1, 0) == b"00") || (opcode(1, 0) == b"11"))
  cond_branch := !opcode(0)
  ctrl_utype := !opcode(4) && opcode(2) && opcode(0)
  ctrl_jal_or_jalr := opcode(4) && opcode(0)
  // PC-relative: true for jal, b*, auipc, ebreak; false for jalr, lui
  ctrl_pc_rel := (opcode(2, 0) == b"000") || (opcode(1, 0) == b"11") ||
    (opcode(4) && opcode(2) && op20) || (opcode(4, 3) == b"00")
  // write to RD: true for OP-IMM, AUIPC, OP, LUI, SYSTEM, JALR, JAL, LOAD
  val rd_op_w = opcode(2) || (!opcode(2) && opcode(4) && opcode(0)) ||
    (!opcode(2) && !opcode(3) && !opcode(0))
  rd_op := rd_op_w

  sh_right := funct3(2)
  bne_or_bge := funct3(0)
  // matches system ops except ecall/ebreak/mret
  val csr_op = opcode(4) && opcode(2) && funct3.|
  ebreak := op20
  ctrl_mret := opcode(4) && opcode(2) && op21 && !funct3.|
  e_op := opcode(4) && opcode(2) && !op21 && !funct3.|
  bufreg_sh_signed := imm30
  // true for sub, b*, slt*; false for add*
  alu_sub := funct3(1) || funct3(0) || (opcode(3) && imm30) || opcode(4)

  // true for mtvec, mscratch, mepc and mtval; false for mstatus, mie, mcause
  val csr_valid = op20 || (op26 && !op21)
  rd_csr_en := csr_op
  csr_en := csr_op && csr_valid
  csr_mstatus_en := csr_op && !op26 && !op22 && !op20
  csr_mie_en := csr_op && !op26 && op22 && !op20
  csr_mcause_en := csr_op && op21 && !op20
  csr_source := funct3(1, 0)
  csr_d_sel := funct3(2)
  val csr_imm_en_w = opcode(4) && opcode(2) && funct3(2)
  csr_imm_en := csr_imm_en_w
  csr_addr := (op26 && op20, !op26 || op21).toBits

  alu_cmp_eq := funct3(2, 1) == b"00"
  alu_cmp_sig := !((funct3(0) && funct3(1)) || (funct3(1) && funct3(2)))
  mem_cmd := opcode(3)
  mem_signed := !funct3(2)
  mem_half := funct3(0)
  alu_bool_op := funct3(1, 0)

  immdec_ctrl := (
    opcode(4),
    opcode(4) && !opcode(0),
    (opcode(1, 0) == b"00") || (opcode(2, 1) == b"00"),
    opcode(3, 0) == b"1000"
  ).toBits
  immdec_en := (
    opcode(4) || opcode(3) || opcode(2) || !opcode(0), // B I J S U
    (opcode(4) && opcode(2)) || !opcode(3) || opcode(0), // I J U
    (opcode(2, 1) == b"01") || (opcode(2) && opcode(0)) || csr_imm_en_w, // J U
    !rd_op_w // B S
  ).toBits
  alu_rd_sel := (funct3(2), funct3(2, 1) == b"01", funct3 == b"000").toBits
  // 0 (imm) when OP-IMM; 1 (rs2) when BRANCH or OP
  op_b_source := opcode(3)
end serv_decode
