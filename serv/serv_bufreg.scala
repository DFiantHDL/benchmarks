// DFHDL port of serv_bufreg.v from SERV by Olof Kindgren (see serv_top.scala for the
// port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2019 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_bufreg.v: buffer register for load/store address and shift data (W = 1). Clocked on
  * `i_clk`, no reset. The MDU (`i_mdu_op`), extension (`o_ext_rs1`) and W=4 (`i_cnt_done`,
  * `i_cnt1`) ports are omitted since MDU/aligner/W=4 are not part of this benchmark configuration.
  */
@hw.constraints.timing.clock(portName = "i_clk")
class serv_bufreg extends RTDesign:
  // State
  val i_cnt0 = Bit <> IN
  val i_cnt1 = Bit <> IN
  val i_en = Bit <> IN
  val i_init = Bit <> IN
  val o_lsb = Bits(2) <> OUT
  // Control
  val i_rs1_en = Bit <> IN
  val i_imm_en = Bit <> IN
  val i_clr_lsb = Bit <> IN
  val i_sh_signed = Bit <> IN
  // Data
  val i_rs1 = Bit <> IN
  val i_imm = Bit <> IN
  val o_q = Bit <> OUT
  // External
  val o_dbus_adr = Bits(32) <> OUT

  val c_r = Bit <> VAR.REG init 0
  val data = Bits(32) <> VAR.REG init all(0)

  // single-bit full adder: (i_rs1 & i_rs1_en) + (i_imm & i_imm_en & ~clr_lsb) + carry
  val add_a = i_rs1 && i_rs1_en
  val add_b = i_imm && i_imm_en && !(i_cnt0 && i_clr_lsb)
  val sum = add_a ^ add_b ^ c_r
  val cy = (add_a && add_b) || (add_a && c_r) || (add_b && c_r)

  // make sure carry is cleared before loading new data
  c_r.din := cy && i_en
  if (i_en) data(31, 2).din := (i_init.sel(sum, data(31) && i_sh_signed), data(31, 3)).toBits
  if (i_init.sel(i_cnt0 || i_cnt1, i_en))
    data(1, 0).din := (i_init.sel(sum, data(2)), data(1)).toBits
  o_lsb := data(1, 0)
  o_q := data(0) && i_en
  o_dbus_adr := (data(31, 2), b"00").toBits
end serv_bufreg
