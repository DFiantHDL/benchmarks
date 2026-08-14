// `beh_lib.sv`'s SECDED decoder: checks a 32-bit word against its 7-bit ECC, corrects a single-bit
// error, and flags single and double errors.
//
// Purely combinational, so it carries no clock or reset.
//
// The code is a (39, 32) Hamming code with an overall parity bit. Bits 0..5 of `ecc_check` are the
// Hamming syndrome; bit 6 is the overall parity, which distinguishes a correctable single error
// from an uncorrectable double one. `sed_ded` forces detection-only, used by the I-cache.
//
// Instantiated by ifu_aln_ctl, ifu_ic_mem, ifu_mem_ctl and lsu_ecc.
//
// Upstream: chipsalliance/Cores-VeeR-EH1@915fb34, via RTLMeter designs/VeeR-EH1.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.veer_eh1

import dfhdl.*

class rvecc_decode extends RTDesign:
  val en               = Bit      <> IN
  val din              = Bits(32) <> IN
  val ecc_in           = Bits(7)  <> IN
  val sed_ded          = Bit      <> IN // detection only, no correction; used for the I$
  val dout             = Bits(32) <> OUT
  val ecc_out          = Bits(7)  <> OUT
  val single_ecc_error = Bit      <> OUT
  val double_ecc_error = Bit      <> OUT

  val ecc_check = Bits(7) <> VAR
  for (i <- 0 until 6)
    ecc_check(i) <> hammingGroup(i).map(din(_)).foldLeft[Bit <> VAL](ecc_in(i))(_ ^ _)
  // The overall parity bit.
  ecc_check(6) <> ((din.^ ^ ecc_in.^) & ~sed_ded)

  val syndrome = ecc_check(5, 0).uint

  single_ecc_error <> (en & (ecc_check != all(0)) & ecc_check(6))
  double_ecc_error <> (en & (ecc_check != all(0)) & ~ecc_check(6))

  // One-hot mask of the syndrome's target bit. Syndrome 0 means "no error", so mask bit i-1 is
  // syndrome i -- the offset in the baseline's `genvar i=1; i<40` loop.
  val error_mask = Bits(39) <> VAR
  for (i <- 1 until 40) error_mask(i - 1) <> (syndrome == i)

  // Data and check bits interleaved into Hamming positions.
  val din_plus_parity: Bits[39] <> VAL =
    (
      ecc_in(6),
      din(31, 26),
      ecc_in(5),
      din(25, 11),
      ecc_in(4),
      din(10, 4),
      ecc_in(3),
      din(3, 1),
      ecc_in(2),
      din(0),
      ecc_in(1, 0)
    )

  val dout_plus_parity = single_ecc_error.sel(error_mask ^ din_plus_parity, din_plus_parity)

  // ... and de-interleaved back out.
  dout <>
    (
      dout_plus_parity(37, 32),
      dout_plus_parity(30, 16),
      dout_plus_parity(14, 8),
      dout_plus_parity(6, 4),
      dout_plus_parity(2)
    )

  ecc_out <>
    (
      dout_plus_parity(38) ^ (ecc_check == h"7'40"),
      dout_plus_parity(31),
      dout_plus_parity(15),
      dout_plus_parity(7),
      dout_plus_parity(3),
      dout_plus_parity(1, 0)
    )
end rvecc_decode
