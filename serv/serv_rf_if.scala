// DFHDL port of serv_rf_if.v from SERV by Olof Kindgren (see serv_top.scala for the
// port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2019 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_rf_if.v: register file access multiplexing, including the four RF-resident CSRs (WITH_CSR =
  * 1).
  */
class serv_rf_if extends RTDesign:
  val cnt_en = Bit <> IN
  val rdata0 = Bit <> IN
  val rdata1 = Bit <> IN
  // trap interface
  val trap = Bit <> IN
  val mret = Bit <> IN
  val mepc = Bit <> IN
  val mtval_pc = Bit <> IN
  val bufreg_q = Bit <> IN
  val bad_pc = Bit <> IN
  // CSR write port
  val csr_en = Bit <> IN
  val csr_addr = Bits(2) <> IN
  val csr_in = Bit <> IN
  // RD write port
  val rd_wen = Bit <> IN
  val rd_waddr = Bits(5) <> IN
  val ctrl_rd = Bit <> IN
  val alu_rd = Bit <> IN
  val rd_alu_en = Bit <> IN
  val csr_rd = Bit <> IN
  val rd_csr_en = Bit <> IN
  val mem_rd = Bit <> IN
  val rd_mem_en = Bit <> IN
  // read ports
  val rs1_raddr = Bits(5) <> IN
  val rs2_raddr = Bits(5) <> IN
  // RF side
  val wreg0 = Bits(6) <> OUT
  val wreg1 = Bits(6) <> OUT
  val wen0 = Bit <> OUT
  val wen1 = Bit <> OUT
  val wdata0 = Bit <> OUT
  val wdata1 = Bit <> OUT
  val rreg0 = Bits(6) <> OUT
  val rreg1 = Bits(6) <> OUT
  val rs1 = Bit <> OUT
  val rs2 = Bit <> OUT
  val csr = Bit <> OUT
  val csr_pc = Bit <> OUT

  val rd_wen_masked = rd_wen && rd_waddr.|
  val rd = (rd_alu_en && alu_rd) || (rd_csr_en && csr_rd) || (rd_mem_en && mem_rd) || ctrl_rd
  val mtval = mtval_pc.sel(bad_pc, bufreg_q)
  // port 0: mtval during traps, rd otherwise; port 1: mepc during traps, csr otherwise
  wdata0 := trap.sel(mtval, rd)
  wdata1 := trap.sel(mepc, csr_in)
  // GPRs at 0-31; then mscratch(32) mtvec(33) mepc(34) mtval(35)
  wreg0 := trap.sel(b"100011", (0, rd_waddr).toBits)
  wreg1 := trap.sel(b"100010", (b"1000", csr_addr).toBits)
  wen0 := cnt_en && (trap || rd_wen_masked)
  wen1 := cnt_en && (trap || csr_en)
  rreg0 := (0, rs1_raddr).toBits
  // rreg1 source: rs2 (normal) / csr address (CSR access) / MTVEC (trap) / MEPC (mret)
  val sel_rs2 = !(trap || mret || csr_en)
  val rreg1_low = (mret, trap).toBits | csr_en.sel(csr_addr, b"00") |
    sel_rs2.sel(rs2_raddr(1, 0), b"00")
  rreg1 := (!sel_rs2, sel_rs2.sel(rs2_raddr(4, 2), b"000"), rreg1_low).toBits
  rs1 := rdata0
  rs2 := rdata1
  csr := rdata1 && csr_en
  csr_pc := rdata1
end serv_rf_if
