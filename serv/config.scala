// Global clock/reset configuration for the Servant benchmark package.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

// The Servant SoC top (`servant`) and the simulation tops clock/reset on `wb_clk`/`wb_rst`, so
// those are the package-wide DFHDL defaults. The internal serv/servile modules use different
// clock/reset port names (`clk`/`i_clk`, `i_rst`/`i_wb_rst`), which they set individually via
// per-module `@hw.constraints.timing.clock/reset(portName = ...)` annotations that override these
// defaults; modules without a reset carry a clock-only annotation.
given options.ElaborationOptions.DefaultClkCfg =
  hw.constraints.timing.clock(portName = "wb_clk")
given options.ElaborationOptions.DefaultRstCfg =
  hw.constraints.timing.reset(portName = "wb_rst")
