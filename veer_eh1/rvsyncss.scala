// `beh_lib.sv`'s two-flop synchronizer.
//
// The baseline chains two `rvdff`s; nothing here needs those flops placed individually, so this is
// a two-deep delay. The `init` is `rvdff`'s asynchronous active-low reset to 0.
//
// Both call sites are single-domain (dec_tlu_ctl at WIDTH=6, pic_ctrl at WIDTH=TOTAL_INT-1), so
// this is a two-cycle delay rather than a CDC crossing. Kept at two stages regardless, since
// collapsing it would change the pipeline depth the callers were written against.
//
// Upstream: chipsalliance/Cores-VeeR-EH1@915fb34, via RTLMeter designs/VeeR-EH1.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.veer_eh1

import dfhdl.*

class rvsyncss(val WIDTH: Int <> CONST = 251) extends RTDesign:
  val din  = Bits(WIDTH) <> IN
  val dout = Bits(WIDTH) <> OUT

  dout <> din.reg(2, init = all(0))
end rvsyncss
