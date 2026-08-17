// What `beh_lib.sv` leaves over once its modules have been placed.
//
// The flop and clock-gate primitives are not here: each is written inline at its use site, and
// under the pinned config there is no clock gating at all to model (see the README on
// `RV_FPGA_OPTIMIZE`). The combinational helpers are each a design in a same-named file. What
// remains here is what several of them share.
//
// Upstream: chipsalliance/Cores-VeeR-EH1@915fb34, via RTLMeter designs/VeeR-EH1.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.veer_eh1

import dfhdl.*

/** The baseline declares `[`RV_BTB_ADDR_HI:`RV_BTB_ADDR_LO]`; only the hashes below need it. */
private val BTB_INDEX_WIDTH: Int <> CONST = RV_BTB_ADDR_HI - RV_BTB_ADDR_LO + 1

/** The baseline declares `[`RV_BHT_ADDR_HI:`RV_BHT_ADDR_LO]`. */
private val BHT_HASH_WIDTH: Int <> CONST = RV_BHT_ADDR_HI - RV_BHT_ADDR_LO + 1

/** The (39, 32) Hamming code's parity groups: the data bits each check bit covers.
  *
  * `rvecc_encode` XORs each group to produce a check bit; `rvecc_decode` XORs the same group
  * against the received check bit to produce that bit of the syndrome. The baseline writes the two
  * out separately, as six `assign`s each, and they must agree exactly -- so they are transcribed
  * once here instead, where a transposed index cannot differ between them.
  */
private[veer_eh1] val hammingGroup = Vector(
  Vector(0, 1, 3, 4, 6, 8, 10, 11, 13, 15, 17, 19, 21, 23, 25, 26, 28, 30),
  Vector(0, 2, 3, 5, 6, 9, 10, 12, 13, 16, 17, 20, 21, 24, 25, 27, 28, 31),
  Vector(1, 2, 3, 7, 8, 9, 10, 14, 15, 16, 17, 22, 23, 24, 25, 29, 30, 31),
  Vector(4, 5, 6, 7, 8, 9, 10, 18, 19, 20, 21, 22, 23, 24, 25),
  Vector(11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25),
  Vector(26, 27, 28, 29, 30, 31)
)

// The BTB/BHT hashes. Each is a single `assign` over configuration macros -- `rvbtb_ghr_hash`'s
// body *is* the `RV_BHT_HASH_STRING` macro -- so they are methods rather than designs. Each still
// proves against its baseline module by wrapping it in a design with that module's port list.

/** `beh_lib.sv`'s BTB tag hash: folds the upper PC bits down to a branch tag.
  *
  * `RV_BTB_BTAG_FOLD` is defined by the pinned config, so this is the two-term fold; the unfolded
  * branch XORs three slices.
  */
@inline def rvbtb_tag_hash(
    pc: BitsHL[31, 1] <> VAL
): Bits[RV_BTB_BTAG_SIZE.type] <> DFRET =
  pc(RV_BTB_ADDR_HI + 2 * RV_BTB_BTAG_SIZE, RV_BTB_ADDR_HI + RV_BTB_BTAG_SIZE + 1) ^
    pc(RV_BTB_ADDR_HI + RV_BTB_BTAG_SIZE, RV_BTB_ADDR_HI + 1)

/** `beh_lib.sv`'s BTB index hash: folds three PC slices into the BTB index.
  *
  * `RV_BTB_FOLD2_INDEX_HASH` is not defined by the pinned config, so all three slices participate.
  */
@inline def rvbtb_addr_hash(
    pc: BitsHL[31, 1] <> VAL
): Bits[BTB_INDEX_WIDTH.type] <> DFRET =
  pc(RV_BTB_INDEX1_HI, RV_BTB_INDEX1_LO) ^ pc(RV_BTB_INDEX2_HI, RV_BTB_INDEX2_LO) ^
    pc(RV_BTB_INDEX3_HI, RV_BTB_INDEX3_LO)

/** `beh_lib.sv`'s BHT hash: mixes the BTB index with the global history register.
  *
  * The baseline defers this to `RV_BHT_HASH_STRING`, a macro the config script generates because
  * the function varies with the branch-predictor configuration. For the pinned config it expands to
  * `{ghr[3:2] ^ {ghr[4], 1'b0}, hashin[5:4] ^ ghr[1:0]}`, which is what is written here. Regenerate
  * this body if the BP configuration ever moves.
  */
@inline def rvbtb_ghr_hash(
    hashin: Bits[BTB_INDEX_WIDTH.type] <> VAL,
    ghr: Bits[RV_BHT_GHR_SIZE.type] <> VAL
): Bits[BHT_HASH_WIDTH.type] <> DFRET =
  ((ghr(3, 2) ^ (ghr(4), b"0")), hashin ^ ghr(1, 0))
