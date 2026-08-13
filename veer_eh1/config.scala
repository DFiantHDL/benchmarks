// Global clock/reset configuration for the VeeR-EH1 benchmark package.
//
// Every VeeR-EH1 module clocks on `clk` and resets on `rst_l`, an ASYNCHRONOUS ACTIVE-LOW reset:
// the flop primitives in beh_lib.sv are all `always_ff @(posedge clk or negedge rst_l)` resetting
// to 0. Those are the package-wide defaults, so no module needs an annotation merely to spell
// them; a module that departs from them overrides per-module.
//
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.veer_eh1

import dfhdl.*

given options.ElaborationOptions.DefaultClkCfg =
  hw.constraints.timing.clock(portName = "clk")
given options.ElaborationOptions.DefaultRstCfg =
  hw.constraints.timing.reset(mode = _.async, active = _.low, portName = "rst_l")
