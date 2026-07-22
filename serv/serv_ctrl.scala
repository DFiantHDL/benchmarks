// DFHDL port of serv_ctrl.v from SERV by Olof Kindgren (see serv_top.scala for the
// port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2018 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_ctrl.v: the serial program counter (W = 1, WITH_CSR = 1, RESET_PC = 0). */
class serv_ctrl extends RTDesign:
  val wb_rst = Bit <> IN
  val pc_en = Bit <> IN
  val cnt12to31 = Bit <> IN
  val cnt0 = Bit <> IN
  val cnt1 = Bit <> IN
  val cnt2 = Bit <> IN
  val jump = Bit <> IN
  val jal_or_jalr = Bit <> IN
  val utype = Bit <> IN
  val pc_rel = Bit <> IN
  val trap = Bit <> IN // trap | mret
  val imm = Bit <> IN
  val bufreg_q = Bit <> IN
  val csr_pc = Bit <> IN
  val rd = Bit <> OUT
  val bad_pc = Bit <> OUT
  val ibus_adr = Bits(32) <> OUT.REG init all(0)

  val pc_plus_4_cy_r = Bit <> VAR.REG init 0
  val pc_plus_offset_cy_r = Bit <> VAR.REG init 0

  // the serial PC bit is ibus_adr(0); kept inline because naming a bit-select of an
  // assignable port would register as a connection into it
  val plus_4 = cnt2 // no compressed support: pc increments by 4
  val pc_plus_4 = ibus_adr(0) ^ plus_4 ^ pc_plus_4_cy_r
  val pc_plus_4_cy = (ibus_adr(0) && plus_4) || (ibus_adr(0) && pc_plus_4_cy_r) ||
    (plus_4 && pc_plus_4_cy_r)
  val offset_a = pc_rel && ibus_adr(0)
  val offset_b = utype.sel(imm && cnt12to31, bufreg_q)
  val pc_plus_offset = offset_a ^ offset_b ^ pc_plus_offset_cy_r
  val pc_plus_offset_cy = (offset_a && offset_b) || (offset_a && pc_plus_offset_cy_r) ||
    (offset_b && pc_plus_offset_cy_r)
  val pc_plus_offset_aligned = pc_plus_offset && !cnt0
  val new_pc =
    trap.sel(csr_pc && !(cnt0 || cnt1), jump.sel(pc_plus_offset_aligned, pc_plus_4))
  bad_pc := pc_plus_offset_aligned
  rd := (utype && pc_plus_offset_aligned) || (pc_plus_4 && jal_or_jalr)
  pc_plus_4_cy_r.din := pc_en && pc_plus_4_cy
  pc_plus_offset_cy_r.din := pc_en && pc_plus_offset_cy
  if (pc_en || wb_rst)
    ibus_adr.din := wb_rst.sel(h"00000000", (new_pc, ibus_adr(31, 1)).toBits)
end serv_ctrl
