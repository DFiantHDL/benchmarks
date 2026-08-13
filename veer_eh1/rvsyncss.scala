// `beh_lib.sv`'s two-flop synchronizer.
//
// The baseline chains two `rvdff`s, but nothing here needs those flops placed individually, so what
// this design wants to say is a two-deep delay:
//
//   dout <> din.reg(2, init = all(0))
//
// That crashes the compiler when the width comes from a parameter, as it does here (DFHDL#485), so
// the registers are declared and chained instead. Restore the one-liner when #485 is fixed.
//
// The `init all(0)` is what `rvdff`'s asynchronous active-low reset to 0 becomes, taking
// `clk`/`rst_l` from config.scala.
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
  val dout = Bits(WIDTH) <> OUT.REG init all(0)

  val din_ff1 = Bits(WIDTH) <> VAR.REG init all(0)

  din_ff1.din := din
  dout.din    := din_ff1
end rvsyncss
