// `beh_lib.sv`'s load/store address adder: `rs1` plus a signed 12-bit offset.
//
// Purely combinational, so it carries no clock or reset.
//
// The low 12 bits are added directly; the upper 20 are incremented, decremented or kept depending
// on the carry out and the offset's sign, which avoids carrying a 32-bit adder.
//
// Instantiated by lsu_lsc_ctl.
//
// Upstream: chipsalliance/Cores-VeeR-EH1@915fb34, via RTLMeter designs/VeeR-EH1.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.veer_eh1

import dfhdl.*

class rvlsadder extends RTDesign:
  val rs1    = Bits(32) <> IN
  val offset = Bits(12) <> IN
  val dout   = Bits(32) <> OUT

  val sum     = rs1(11, 0) +^ offset
  val cout    = sum(12)
  val sign    = offset(11)
  val rs1_hi  = rs1(31, 12)
  val rs1_inc = rs1_hi + 1
  val rs1_dec = rs1_hi - 1

  dout(11, 0)  <> sum(11, 0)
  dout(31, 12) <> ((~(sign ^ cout)).repeat(20) & rs1_hi) |
    ((~sign & cout).repeat(20) & rs1_inc) |
    ((sign & ~cout).repeat(20) & rs1_dec)
end rvlsadder
