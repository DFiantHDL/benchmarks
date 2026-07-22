// DFHDL port of serv_ctrl.v from SERV by Olof Kindgren (see serv_top.scala for the
// port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2018 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_ctrl.v: the serial program counter (W = 1, WITH_CSR = 1, RESET_PC = 0). Clocked on `clk`
  * with the synchronous MINI reset `i_rst`; the register power-up/reset value is RESET_PC = 0, so
  * the `o_ibus_adr <= i_rst ? RESET_PC` mux collapses into the register reset. The compressed
  * `i_iscomp` port is omitted (no compressed-ISA support).
  */
@hw.constraints.timing.clock(portName = "clk")
@hw.constraints.timing.reset(portName = "i_rst")
class serv_ctrl extends RTDesign:
  // State
  val i_pc_en = Bit <> IN
  val i_cnt12to31 = Bit <> IN
  val i_cnt0 = Bit <> IN
  val i_cnt1 = Bit <> IN
  val i_cnt2 = Bit <> IN
  // Control
  val i_jump = Bit <> IN
  val i_jal_or_jalr = Bit <> IN
  val i_utype = Bit <> IN
  val i_pc_rel = Bit <> IN
  val i_trap = Bit <> IN
  // Data
  val i_imm = Bit <> IN
  val i_buf = Bit <> IN
  val i_csr_pc = Bit <> IN
  val o_rd = Bit <> OUT
  val o_bad_pc = Bit <> OUT
  // External
  val o_ibus_adr = Bits(32) <> OUT.REG init all(0)

  val pc_plus_4_cy_r = Bit <> VAR.REG init 0
  val pc_plus_offset_cy_r = Bit <> VAR.REG init 0

  // the serial PC bit is o_ibus_adr(0); kept inline because naming a bit-select of an
  // assignable port would register as a connection into it
  val plus_4 = i_cnt2 // no compressed support: pc increments by 4
  val pc_plus_4 = o_ibus_adr(0) ^ plus_4 ^ pc_plus_4_cy_r
  val pc_plus_4_cy = (o_ibus_adr(0) && plus_4) || (o_ibus_adr(0) && pc_plus_4_cy_r) ||
    (plus_4 && pc_plus_4_cy_r)
  val offset_a = i_pc_rel && o_ibus_adr(0)
  val offset_b = i_utype.sel(i_imm && i_cnt12to31, i_buf)
  val pc_plus_offset = offset_a ^ offset_b ^ pc_plus_offset_cy_r
  val pc_plus_offset_cy = (offset_a && offset_b) || (offset_a && pc_plus_offset_cy_r) ||
    (offset_b && pc_plus_offset_cy_r)
  val pc_plus_offset_aligned = pc_plus_offset && !i_cnt0
  val new_pc =
    i_trap.sel(i_csr_pc && !(i_cnt0 || i_cnt1), i_jump.sel(pc_plus_offset_aligned, pc_plus_4))
  o_bad_pc := pc_plus_offset_aligned
  o_rd := (i_utype && pc_plus_offset_aligned) || (pc_plus_4 && i_jal_or_jalr)
  pc_plus_4_cy_r.din := i_pc_en && pc_plus_4_cy
  pc_plus_offset_cy_r.din := i_pc_en && pc_plus_offset_cy
  if (i_pc_en) o_ibus_adr.din := (new_pc, o_ibus_adr(31, 1)).toBits
end serv_ctrl
