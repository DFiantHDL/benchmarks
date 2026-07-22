// DFHDL port of serv_mem_if.v from SERV by Olof Kindgren (see serv_top.scala for the
// port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2018 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_mem_if.v: load/store byte lane handling (W = 1, WITH_CSR = 1). */
class serv_mem_if extends RTDesign:
  val bytecnt = Bits(2) <> IN
  val lsb = Bits(2) <> IN
  val mem_signed = Bit <> IN
  val mem_word = Bit <> IN
  val mem_half = Bit <> IN
  val bufreg2_q = Bit <> IN
  val rd = Bit <> OUT
  val wb_sel = Bits(4) <> OUT
  val misalign = Bit <> OUT

  val signbit = Bit <> VAR.REG init 0
  val dat_valid = mem_word || (bytecnt == b"00") || (mem_half && !bytecnt(1))
  rd := dat_valid.sel(bufreg2_q, mem_signed && signbit)
  wb_sel := (
    (lsb == b"11") || mem_word || (mem_half && lsb(1)),
    (lsb == b"10") || mem_word,
    (lsb == b"01") || mem_word || (mem_half && !lsb(1)),
    lsb == b"00"
  ).toBits
  if (dat_valid) signbit.din := bufreg2_q
  misalign := (lsb(0) && (mem_word || mem_half)) || (lsb(1) && mem_word)
end serv_mem_if
