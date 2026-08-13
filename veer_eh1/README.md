# VeeR-EH1

DFHDL port of the [VeeR EH1](https://github.com/chipsalliance/Cores-VeeR-EH1) RISC-V core, a
32-bit in-order **9-stage dual-issue superscalar** machine with ICCM, DCCM and an instruction
cache.

| | |
|---|---|
| Upstream | [chipsalliance/Cores-VeeR-EH1](https://github.com/chipsalliance/Cores-VeeR-EH1) @ `915fb34a5b38ef14c5d5c05431765ad8b912bc34` |
| Imported via | RTLMeter `designs/VeeR-EH1`, so the port and the Verilator reference are the same RTL |
| License | Apache-2.0 ([LICENSE-VeeR-EH1](LICENSE-VeeR-EH1)) |
| Configuration | RTLMeter **`default`** (`src/default/common_defines.vh`) |
| Domain | `RTDesign` |

This is a derivative work and carries its origin license, per the benchmarks repo's rule. It must
not migrate into the main DFHDL repository.

**Status: in progress.** The configuration and type layer are in place; the design modules are
not yet ported.

## Pinned configuration

The `default` configuration, resolved. `hiperf` and `asic` are out of scope for the first pass.

| | |
|---|---|
| Bus | AXI4 (`RV_BUILD_AXI4 1`; no AHB-Lite) |
| ICCM | enabled, **512 KB**, 8 banks of `ram_16384x39` |
| DCCM | enabled, 64 KB, 8 banks of `ram_2048x39` |
| I-cache | enabled, 16 KB, `ram_256x34` data + `ram_64x21` tag |
| Branch prediction | BTB 32 entries, BHT 128, 4-deep return stack |
| PIC | 8 external interrupts |
| Load/store | 8 store-buffer entries, 8 non-blocking loads |
| Assertions | **off** (`ASSERT_ON` is `` `undef ``-ed on the last line of `common_defines.vh`) |
| Clock gating | **none** (`RV_FPGA_OPTIMIZE` is defined; see below) |

### `RV_FPGA_OPTIMIZE` is defined, so the pinned build has no clock gating

This is the single most consequential fact about the pinned configuration, and it is easy to miss
because the source is full of clock-gating machinery that this build does not compile:

- `rvclkhdr` is inside `` `ifndef RV_FPGA_OPTIMIZE ``, so **the module does not exist**, and every
  instantiation of it is itself inside an `` `ifndef ``.
- `rvoclkhdr` degenerates to `assign l1clk = clk` -- a wire.
- `rvdffe` degenerates to `rvdffs`: a plain enable flop on the root clock, no ICG.
- `rvdff_fpga` / `rvdffs_fpga` / `rvdffsc_fpga` take the `rawclk` + `clken` path, so their `clken`
  is a genuine **register enable**, not a clock.

So every `*_clk` a module receives in this build is the root `clk`, and the whole design is
combinational logic plus enabled registers on one clock. That is the RT register model exactly,
which is why the port is `RTDesign` and why it declares no gated clocks anywhere.

## Layout

| file | baseline counterpart |
|---|---|
| [`config.scala`](config.scala) | package-wide clock/reset defaults (`clk`, async active-low `rst_l`) |
| [`defines.scala`](defines.scala) | `src/default/common_defines.vh` -- a **global** include, so these are top-level package definitions |
| [`globals.scala`](globals.scala) | `src/global.h` -- a **body-scoped** include, so this is an `object` that each design `export`s |

### Why the two constant files have different shapes

The two Verilog headers do not scope the same way, so they do not get the same Scala form.

`common_defines.vh` is a global include: its macros are visible to every module. Scala top-level
definitions in the package match that -- visible to every file with no import.

`global.h` is included **inside** 20 module bodies, which makes its localparams *members of each
of those modules*. Only `export` reproduces that, because it puts the names on the type:

```scala
class lsu_dccm_mem extends RTDesign:
  export globals.*          // `include "global.h"
  // DCCM_BITS, DCCM_NUM_BANKS, ... are now members of this design
```

A plain `import` would bring the names into scope without making them members, which is a weaker
and non-equivalent relation. The forms are not interchangeable in the other direction either: a
package cannot be an export target at all.

### The `<> CONST` ascriptions are load-bearing

A plain Scala `Int` folds into a literal and the name is gone. Ascribed `: Int <> CONST`, the
constant reaches the IR and the emitter writes it as a named `parameter int`, **preserving the
definition chain** the baseline had:

```systemverilog
parameter int RV_DCCM_FDATA_WIDTH = 39;                    // `define RV_DCCM_FDATA_WIDTH 39
parameter int DCCM_FDATA_WIDTH    = RV_DCCM_FDATA_WIDTH;   // localparam DCCM_FDATA_WIDTH = `RV_...
parameter logic [31:0] RV_ICCM_SADR = 32'hee000000;        // `define RV_ICCM_SADR 32'hee000000
```

and every declaration derived from one keeps the name rather than a magic number
(`input wire logic [DCCM_FDATA_WIDTH - 1:0] dccm_wd`). That is what makes the generated HDL
diffable against the gold, which is the first rung of the verification ladder. Only constants the
elaborated design actually references reach `<Top>_defs.svh`, so declaring the full set costs
nothing in the output.

Macros that are only `` `ifdef ``-tested (`RV_FPGA_OPTIMIZE`, `ASSERT_ON`) are plain Scala
`Boolean`s and macros that name an SRAM cell are plain Scala `String`s: both select code at
elaboration and must not reach the IR.

## Deviations from the baseline

Deliberate differences, each argued non-architectural. To be completed as the port proceeds.

- **`scan_mode` is dropped from every port list.** It is tied to 0 by the testbench and only ever
  fed the ICG cells, which this configuration does not build.
- **Assertions, DFT logic and the JTAG/DMI subsystem are not ported.** `tb_top.sv:714-717` ties
  `jtag_tck/tms/tdi` to 0 and holds `jtag_trst_n` at 0, so the TAP/DMI path cannot change
  architectural state; `dbg` itself is ported because its outputs feed the TLU.
- **The combinational `rv*` helpers from `beh_lib.sv` are Scala methods, not designs**, so they
  inline at their call sites and the generated HDL is flatter than the baseline. This changes
  module count and net names, not behaviour.

## Workloads

The RTLMeter tests, run to a fixed cycle count with the architectural state line compared
bit-for-bit across DFacsimile tiers and Verilator: `hello` (sanity), `cmark`, `dhry`.
