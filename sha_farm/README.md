# SHAFarm

`n` distinctly seeded always-active SHA-256 compression cores (one round per cycle, ~850 FFs per
core) XOR-reduced into one 128-bit output; n = 32 is ~27k FFs. Sizes 1/8/32/64 sweep a
simulator's large-netlist behavior. Named tops `SHAFarm1/8/64` exist so each size can be
committed to Verilog for the external harness.

- **Origin**: written fresh for this repository (secworks/sha256-style round structure)
- **License**: Apache 2.0 (repository default)
- **Run**: `benchmarks/runMain dfhdl.benchmarks.sha_farm.shaFarmBench` (full sweep) or
  `... .shaProfile [n] [cycles]` (single config, for profiling)
- **Verilator side**: `verilator/bench_sha.cpp`, one harness for every farm size (`--prefix VTOP`)
