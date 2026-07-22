# DFHDL Benchmarks

Benchmark designs and cross-simulator harnesses for [DFHDL](https://github.com/DFiantHDL/DFHDL).
This repository is wired into the main DFHDL build as the git submodule `benchmarks/`, an sbt
module that depends on `lib` and is never published.

## Layout

Each benchmark is self-contained in its own top-level folder and sub-package: DFHDL sources at
the folder root (`package dfhdl.benchmarks.<work>`), the external simulator harnesses under
`verilator/`, and a `README.md` with provenance and license. `common/` holds shared helpers
(`package dfhdl.benchmarks`). Every work folder is picked up as a source directory by the parent
build, so adding a benchmark is just adding a folder.

## Licensing

The repository as a whole is licensed under Apache 2.0 (see [LICENSE](LICENSE)). Benchmark
designs that are DFHDL ports of other projects are derivative works and carry the license of
their origin, declared in the work folder's `README.md` alongside the upstream repository and
commit, with the upstream license text included in that folder. Ports must never be moved into
the main DFHDL repository.

Current works:

| Work | Origin | License |
|---|---|---|
| [`sha_farm`](sha_farm/) | written fresh (secworks/sha256-style round structure) | Apache 2.0 |
| [`protocol_engine`](protocol_engine/) | written fresh | Apache 2.0 |

## Running

All mains run from the parent DFHDL build (plain `runMain` targets, not tests):

```
sbt "benchmarks/runMain dfhdl.benchmarks.sha_farm.shaFarmBench"
sbt "benchmarks/runMain dfhdl.benchmarks.sha_farm.shaProfile [n] [cycles]"
sbt "benchmarks/runMain dfhdl.benchmarks.protocol_engine.protocolEngineBench"
```

Each main prints throughput (Mcycles/s) per simulation tier plus the architectural state after a
fixed total cycle count; the state line must match bit-for-bit across simulators (DFacsimile
tiers, Verilator, and any other backend) for the numbers to be comparable.

For peak DFacsimile throughput start the JVM with `--add-modules jdk.incubator.vector` (the
parent repo's `.jvmopts` already does this for sbt) — without it the codegen kernel falls back
to a scalar register commit.

## Verilator harnesses

Each work's `verilator/` folder holds the C++ main for the external comparison, verilated with
`--prefix VTOP` so one harness serves every top of that work. From the committed Verilog under
`sandbox/<Top>/hdl` (produced by the benchmark mains above):

```
verilator -O3 --cc --prefix VTOP --exe -Mdir obj_dir --top-module SHAFarm64 \
  -Ihdl SHAFarm64.sv SHA256Unit.sv ../sha_farm/verilator/bench_sha.cpp
make -C obj_dir -f VTOP.mk VTOP OPT_FAST="-O3 -march=native"
obj_dir/VTOP <warmup_cycles> <timed_cycles>
```

Harnesses use the two-eval clock loop (posedge + negedge per cycle) and print the same state
line format as the DFacsimile mains.
