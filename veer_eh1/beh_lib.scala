// What `beh_lib.sv` leaves over once its modules have been placed.
//
// The flop and clock-gate primitives are not here. They carry no logic of their own, so each is
// written inline as an RT register idiom at its use site rather than instantiated as a design --
// `rvdffe #(W) f (.en(e), .din(d), .dout(q))` is `if (e) q.din := d`. Under the pinned config there
// is no clock gating at all to model (see the README on `RV_FPGA_OPTIMIZE`).
//
// The combinational helpers are each a design in a same-named file, so that each can be proven
// against its baseline module on its own. What remains here is what two of them *share*.
//
// Upstream: chipsalliance/Cores-VeeR-EH1@915fb34, via RTLMeter designs/VeeR-EH1.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.veer_eh1

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
