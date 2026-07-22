// DFHDL port of serv_bufreg.v from SERV by Olof Kindgren (see serv_top.scala for the
// port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2019 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_bufreg.v: buffer register for load/store address and shift data (W = 1). */
class serv_bufreg extends RTDesign:
  val cnt0 = Bit <> IN
  val cnt1 = Bit <> IN
  val en = Bit <> IN
  val init_stage = Bit <> IN
  val rs1_en = Bit <> IN
  val imm_en = Bit <> IN
  val clr_lsb = Bit <> IN
  val sh_signed = Bit <> IN
  val rs1 = Bit <> IN
  val imm = Bit <> IN
  val lsb = Bits(2) <> OUT
  val q = Bit <> OUT
  val dbus_adr = Bits(32) <> OUT

  val c_r = Bit <> VAR.REG init 0
  val data = Bits(32) <> VAR.REG init all(0)

  // single-bit full adder: (rs1 & rs1_en) + (imm & imm_en & ~clr_lsb) + carry
  val add_a = rs1 && rs1_en
  val add_b = imm && imm_en && !(cnt0 && clr_lsb)
  val sum = add_a ^ add_b ^ c_r
  val cy = (add_a && add_b) || (add_a && c_r) || (add_b && c_r)

  // make sure carry is cleared before loading new data
  c_r.din := cy && en
  if (en) data(31, 2).din := (init_stage.sel(sum, data(31) && sh_signed), data(31, 3)).toBits
  if (init_stage.sel(cnt0 || cnt1, en))
    data(1, 0).din := (init_stage.sel(sum, data(2)), data(1)).toBits
  lsb := data(1, 0)
  q := data(0) && en
  dbus_adr := (data(31, 2), b"00").toBits
end serv_bufreg
