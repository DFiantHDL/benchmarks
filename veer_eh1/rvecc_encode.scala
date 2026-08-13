// `beh_lib.sv`'s SECDED encoder: produces the 7-bit ECC for a 32-bit word.
//
// Purely combinational, so it carries no clock or reset.
//
// Bits 5:0 are the (39, 32) Hamming check bits, one per parity group; bit 6 is the overall parity
// over the data and those six. `rvecc_decode` inverts this.
//
// Instantiated by ifu_ic_mem, ifu_mem_ctl and lsu_ecc.
//
// Upstream: chipsalliance/Cores-VeeR-EH1@915fb34, via RTLMeter designs/VeeR-EH1.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.veer_eh1

import dfhdl.*

class rvecc_encode extends RTDesign:
  val din     = Bits(32) <> IN
  val ecc_out = Bits(7)  <> OUT

  // Every bit is driven separately, so this is a variable. Unlike the decoder's syndrome there is
  // no received check bit to seed the chain with, so this reduces rather than folds.
  val ecc_out_temp = Bits(6) <> VAR
  for (i <- 0 until 6)
    ecc_out_temp(i) <> hammingGroup(i).map(din(_)).reduce[Bit <> VAL](_ ^ _)

  ecc_out <> (din.^ ^ ecc_out_temp.^, ecc_out_temp)
end rvecc_encode
