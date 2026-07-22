// DFHDL port of serv_mem_if.v from SERV by Olof Kindgren (see serv_top.scala for the
// port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2018 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_mem_if.v: load/store byte lane handling (W = 1, WITH_CSR = 1). Clocked on `i_clk`, no
  * reset. The MDU port (`i_mdu_op`) is omitted (no MDU).
  */
@hw.constraints.timing.clock(portName = "i_clk")
class serv_mem_if extends RTDesign:
  // State
  val i_bytecnt = Bits(2) <> IN
  val i_lsb = Bits(2) <> IN
  val o_misalign = Bit <> OUT
  // Control
  val i_signed = Bit <> IN
  val i_word = Bit <> IN
  val i_half = Bit <> IN
  // Data
  val i_bufreg2_q = Bit <> IN
  val o_rd = Bit <> OUT
  // External interface
  val o_wb_sel = Bits(4) <> OUT

  val signbit = Bit <> VAR.REG init 0
  val dat_valid = i_word || (i_bytecnt == b"00") || (i_half && !i_bytecnt(1))
  o_rd := dat_valid.sel(i_bufreg2_q, i_signed && signbit)
  o_wb_sel := (
    (i_lsb == b"11") || i_word || (i_half && i_lsb(1)),
    (i_lsb == b"10") || i_word,
    (i_lsb == b"01") || i_word || (i_half && !i_lsb(1)),
    i_lsb == b"00"
  ).toBits
  if (dat_valid) signbit.din := i_bufreg2_q
  o_misalign := (i_lsb(0) && (i_word || i_half)) || (i_lsb(1) && i_word)
end serv_mem_if
