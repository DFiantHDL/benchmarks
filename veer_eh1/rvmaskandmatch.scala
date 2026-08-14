// `beh_lib.sv`'s masked comparator, for the debug triggers.
//
// Purely combinational, so it carries no clock or reset.
//
// `mask` holds its mask in the *lower* bits: everything at or below the highest set mask bit is
// masked off, and the upper bits must match `data`. An all-ones mask means "match everything", so
// it is excluded and falls back to a full compare.
//
// The genvar loop reads WIDTH, so the parameter is pinned. Harmless here: dec_trigger and
// lsu_trigger both instantiate it at the default 32.
//
// Upstream: chipsalliance/Cores-VeeR-EH1@915fb34, via RTLMeter designs/VeeR-EH1.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.veer_eh1

import dfhdl.*

class rvmaskandmatch(val WIDTH: Int <> CONST = 32) extends RTDesign:
  val mask   = Bits(WIDTH) <> IN // mask, in the lower bit positions
  val data   = Bits(WIDTH) <> IN // matched against the mask's upper bits
  val masken = Bit         <> IN // 1: mask, 0: full match
  // `match` is a Scala keyword, so the port is back-ticked to keep the baseline's name
  val `match` = Bit <> OUT

  val masken_or_fullmask = masken & ~mask.&

  val matchvec = Bits(WIDTH) <> VAR
  matchvec(0) <> (masken_or_fullmask | mask(0) == data(0))
  for (i <- 1 until WIDTH)
    matchvec(i) <> (mask(i - 1, 0).& & masken_or_fullmask).sel(1, mask(i) == data(i))

  `match` <> matchvec.& // every bit either matched or was masked off
end rvmaskandmatch
