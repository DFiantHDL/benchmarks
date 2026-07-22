// DFHDL port of serv_rf_if.v from SERV by Olof Kindgren (see serv_top.scala for the
// port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2019 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_rf_if.v: register file access multiplexing, including the four RF-resident CSRs (WITH_CSR =
  * 1). Purely combinational, so it carries no clock or reset.
  */
class serv_rf_if extends RTDesign:
  // RF interface
  val i_cnt_en = Bit <> IN
  val o_wreg0 = Bits(6) <> OUT
  val o_wreg1 = Bits(6) <> OUT
  val o_wen0 = Bit <> OUT
  val o_wen1 = Bit <> OUT
  val o_wdata0 = Bit <> OUT
  val o_wdata1 = Bit <> OUT
  val o_rreg0 = Bits(6) <> OUT
  val o_rreg1 = Bits(6) <> OUT
  val i_rdata0 = Bit <> IN
  val i_rdata1 = Bit <> IN
  // Trap interface
  val i_trap = Bit <> IN
  val i_mret = Bit <> IN
  val i_mepc = Bit <> IN
  val i_mtval_pc = Bit <> IN
  val i_bufreg_q = Bit <> IN
  val i_bad_pc = Bit <> IN
  val o_csr_pc = Bit <> OUT
  // CSR interface
  val i_csr_en = Bit <> IN
  val i_csr_addr = Bits(2) <> IN
  val i_csr = Bit <> IN
  val o_csr = Bit <> OUT
  // RD write port
  val i_rd_wen = Bit <> IN
  val i_rd_waddr = Bits(5) <> IN
  val i_ctrl_rd = Bit <> IN
  val i_alu_rd = Bit <> IN
  val i_rd_alu_en = Bit <> IN
  val i_csr_rd = Bit <> IN
  val i_rd_csr_en = Bit <> IN
  val i_mem_rd = Bit <> IN
  val i_rd_mem_en = Bit <> IN
  // read ports
  val i_rs1_raddr = Bits(5) <> IN
  val o_rs1 = Bit <> OUT
  val i_rs2_raddr = Bits(5) <> IN
  val o_rs2 = Bit <> OUT

  val rd_wen = i_rd_wen && i_rd_waddr.|
  val rd = (i_rd_alu_en && i_alu_rd) || (i_rd_csr_en && i_csr_rd) ||
    (i_rd_mem_en && i_mem_rd) || i_ctrl_rd
  val mtval = i_mtval_pc.sel(i_bad_pc, i_bufreg_q)
  // port 0: mtval during traps, rd otherwise; port 1: mepc during traps, csr otherwise
  o_wdata0 := i_trap.sel(mtval, rd)
  o_wdata1 := i_trap.sel(i_mepc, i_csr)
  // GPRs at 0-31; then mscratch(32) mtvec(33) mepc(34) mtval(35)
  o_wreg0 := i_trap.sel(b"100011", (0, i_rd_waddr).toBits)
  o_wreg1 := i_trap.sel(b"100010", (b"1000", i_csr_addr).toBits)
  o_wen0 := i_cnt_en && (i_trap || rd_wen)
  o_wen1 := i_cnt_en && (i_trap || i_csr_en)
  o_rreg0 := (0, i_rs1_raddr).toBits
  // rreg1 source: rs2 (normal) / csr address (CSR access) / MTVEC (trap) / MEPC (mret)
  val sel_rs2 = !(i_trap || i_mret || i_csr_en)
  val rreg1_low = (i_mret, i_trap).toBits | i_csr_en.sel(i_csr_addr, b"00") |
    sel_rs2.sel(i_rs2_raddr(1, 0), b"00")
  o_rreg1 := (!sel_rs2, sel_rs2.sel(i_rs2_raddr(4, 2), b"000"), rreg1_low).toBits
  o_rs1 := i_rdata0
  o_rs2 := i_rdata1
  o_csr := i_rdata1 && i_csr_en
  o_csr_pc := i_rdata1
end serv_rf_if
