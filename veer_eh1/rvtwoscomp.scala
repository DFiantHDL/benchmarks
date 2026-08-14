// `beh_lib.sv`'s two's-complement circuit.
//
// Purely combinational, so it carries no clock or reset.
//
// Negation by inspection rather than by adding one: every bit above the lowest set bit is
// inverted, and everything at or below it is kept. `dout_temp` keeps the baseline's `[WIDTH-1:1]`
// range via `BitsHL`, so the loop indexes it exactly as the baseline's genvar does.
//
// The genvar loop reads WIDTH, so the parameter is pinned. Harmless here: exu_div_ctl is the only
// user and instantiates it at the default 32.
//
// Upstream: chipsalliance/Cores-VeeR-EH1@915fb34, via RTLMeter designs/VeeR-EH1.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.veer_eh1

import dfhdl.*

class rvtwoscomp(val WIDTH: Int <> CONST = 32) extends RTDesign:
  val din  = Bits(WIDTH) <> IN
  val dout = Bits(WIDTH) <> OUT

  // all bits except the LSB, which is always `din(0)`
  val dout_temp = BitsHL(WIDTH - 1, 1) <> VAR
  for (i <- 1 until WIDTH)
    dout_temp(i) <> din(i - 1, 0).|.sel(~din(i), din(i))

  dout <> (dout_temp, din(0))
end rvtwoscomp
