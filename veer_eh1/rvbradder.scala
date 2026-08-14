// `beh_lib.sv`'s branch adder: `pc` plus a signed 13-bit offset.
//
// Purely combinational, so it carries no clock or reset.
//
// The same shape as rvlsadder one step up: only `pc[31:1]` is carried in the pipe, so this adds the
// low 12 bits and picks increment/decrement/keep for the upper 19 from the carry and the sign.
//
// `pc`, `offset` and `dout` are declared on a non-zero base upstream, which `BitsHL` carries, so
// every index below is the baseline's own and the emitted declarations keep the range.
//
// Instantiated 8 times, by dec_decode_ctl, exu_alu_ctl and ifu_bp_ctl.
//
// Upstream: chipsalliance/Cores-VeeR-EH1@915fb34, via RTLMeter designs/VeeR-EH1.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.veer_eh1

import dfhdl.*

class rvbradder extends RTDesign:
  val pc     = BitsHL(31, 1) <> IN
  val offset = BitsHL(12, 1) <> IN
  val dout   = BitsHL(31, 1) <> OUT

  val sum    = pc(12, 1) +^ offset
  val cout   = sum(12)
  val sign   = offset(12)
  val pc_hi  = pc(31, 13)
  val pc_inc = pc_hi + 1
  val pc_dec = pc_hi - 1

  dout(12, 1)  <> sum(11, 0)
  dout(31, 13) <> ((~(sign ^ cout)).repeat(19) & pc_hi) |
    ((~sign & cout).repeat(19) & pc_inc) |
    ((sign & ~cout).repeat(19) & pc_dec)
end rvbradder
