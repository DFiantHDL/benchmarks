// `beh_lib.sv`'s two-flop synchronizer.
//
// The first *sequential* helper, so this is where the flop idiom that the other 727 flop instances
// use gets written down: `rvdff #(W)` is a `VAR.REG`/`OUT.REG` with `init all(0)`, taking the
// package-wide clock and the asynchronous active-low `rst_l` from config.scala. The baseline resets
// every flop to 0, which is exactly what `init all(0)` under a reset annotation emits.
//
// Both call sites are single-domain (dec_tlu_ctl at WIDTH=6, pic_ctrl at WIDTH=TOTAL_INT-1), so
// this is a two-cycle delay rather than a CDC crossing. Kept as two stages regardless, since
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
