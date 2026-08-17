// The consumer side of VeeR's derived (gated) clocks, one trait per clock.
//
// A derived clock's identity is its domain's design-relative name, so `val active` yields the
// `active_clk` port and unifies with every same-named derived clock in the hierarchy. Declaring
// each one in its own trait lets a design pick up exactly the clocks it consumes:
//
//   class dec_gpr_ctl extends RTDesign, ActiveDomain
//   class lsu_stbuf   extends RTDesign, FreeDomain, LsuStbufC1Domain
//
// and the registers on that clock go in a region of it, which keeps their names bare:
//
//   val bankid = new active.RTRegion:
//     val gpr_bank_id = Bits(1) <> VAR.REG init all(0)
//
// `lsu_clkdomain` is the one module on the other side of this: it *sources* the LSU clocks with
// `RTDerivedClkDomainSrc` (`Clk <> OUT`), so it does not use these traits.
//
// Upstream: chipsalliance/Cores-VeeR-EH1@915fb34, via RTLMeter designs/VeeR-EH1.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.veer_eh1

import dfhdl.*

/** `active_clk` — `veer.sv`'s `rvoclkhdr active_cg`, gated by `active_state`. Carries the flops
  * that cannot gate themselves (`rvdff`/`rvdffs`); self-gating `rvdffe` stays on the root clock.
  */
trait ActiveDomain extends RTDesign:
  val active = new RTDerivedClkDomain {}

/** `free_clk` — `veer.sv`'s `rvoclkhdr free_cg` with `.en(1'b1)`, free-running by construction. */
trait FreeDomain extends RTDesign:
  val free = new RTDerivedClkDomain {}

/** `lsu_free_c2_clk` — sourced by `lsu_clkdomain`, free-running while the LSU has any work. */
trait FreeC2Domain extends RTDesign:
  val lsu_free_c2 = new RTDerivedClkDomain {}

// The clocks `lsu_clkdomain` sources from its `rvclkhdr` arm. That arm is not compiled under
// RV_FPGA_OPTIMIZE, so the source ties them to 0 and every consumer is an `rvdff_fpga` /
// `rvdffs_fpga`, which takes `rawclk` + `clken` and discards its `clk` input. In this build these
// ports are therefore dead: a consumer declares the domain for port-list fidelity and puts its
// flops in its own domain, enabled by the matching `*_clken` input.

/** `lsu_freeze_c2_dc2_clk` — dc2 double-pulse clock, held through a freeze. */
trait LsuFreezeC2Dc2Domain extends RTDesign:
  val lsu_freeze_c2_dc2 = new RTDerivedClkDomain {}

/** `lsu_freeze_c2_dc3_clk` — dc3 double-pulse clock, held through a freeze. */
trait LsuFreezeC2Dc3Domain extends RTDesign:
  val lsu_freeze_c2_dc3 = new RTDerivedClkDomain {}

/** `lsu_dccm_c1_dc3_clk` — dc3 single-pulse clock for the DCCM read-data capture. */
trait LsuDccmC1Dc3Domain extends RTDesign:
  val lsu_dccm_c1_dc3 = new RTDerivedClkDomain {}
