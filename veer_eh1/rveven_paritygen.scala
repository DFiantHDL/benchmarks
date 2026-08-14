// `beh_lib.sv`'s even-parity generator.
//
// Purely combinational, so it carries no clock or reset.
//
// Instantiated by ifu_aln_ctl, ifu_ic_mem and ifu_mem_ctl, at WIDTH 16 and 32-ICACHE_TAG_HIGH.
//
// Upstream: chipsalliance/Cores-VeeR-EH1@915fb34, via RTLMeter designs/VeeR-EH1.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.veer_eh1

import dfhdl.*

class rveven_paritygen(val WIDTH: Int <> CONST = 16) extends RTDesign:
  val data_in    = Bits(WIDTH) <> IN
  val parity_out = Bit         <> OUT // generated even parity

  parity_out <> data_in.^
end rveven_paritygen
