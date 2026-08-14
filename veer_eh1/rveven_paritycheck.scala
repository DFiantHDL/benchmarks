// `beh_lib.sv`'s even-parity checker.
//
// Purely combinational, so it carries no clock or reset.
//
// Instantiated by ifu_aln_ctl, ifu_ic_mem and ifu_mem_ctl.
//
// Upstream: chipsalliance/Cores-VeeR-EH1@915fb34, via RTLMeter designs/VeeR-EH1.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.veer_eh1

import dfhdl.*

class rveven_paritycheck(val WIDTH: Int <> CONST = 16) extends RTDesign:
  val data_in    = Bits(WIDTH) <> IN
  val parity_in  = Bit         <> IN
  val parity_err = Bit         <> OUT

  parity_err <> (data_in.^ ^ parity_in)
end rveven_paritycheck
