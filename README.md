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
| [`serv`](serv/) | port of [olofk/serv](https://github.com/olofk/serv) (via RTLMeter's design copy) | ISC |

## Running

All mains run from the parent DFHDL build (plain `runMain` targets, not tests). `benchRun` runs
every benchmark end to end and prints one combined summary table:

```
sbt "benchmarks/runMain dfhdl.benchmarks.benchRun [--verilator]"
```

The individual suites can also be run on their own (each takes the same `--verilator` flag):

```
sbt "benchmarks/runMain dfhdl.benchmarks.serv.servBench [--verilator]"
sbt "benchmarks/runMain dfhdl.benchmarks.sha_farm.shaFarmBench [--verilator]"
sbt "benchmarks/runMain dfhdl.benchmarks.protocol_engine.protocolEngineBench [--verilator]"
sbt "benchmarks/runMain dfhdl.benchmarks.sha_farm.shaProfile [n] [cycles]"
```

Each main prints throughput (Mcycles/s) per simulation tier plus the architectural state after a
fixed total cycle count; the state line must match bit-for-bit across simulators (DFacsimile
tiers, Verilator, and any other backend) for the numbers to be comparable. A summary table at the
end lists the DFHDL (DFacsimile Codegen) throughput per top, and with `--verilator` adds the
Verilator reference throughput, the DFHDL/Verilator ratio, and a `match`/`DIFF` signature check:

```
+-----------------+------------+----------------+-----------+-----------+
| Benchmark       | DFHDL Mcps | Verilator Mcps | DF / Veri | signature |
+-----------------+------------+----------------+-----------+-----------+
| serv/hello-mini |       7.65 |           6.11 |     1.25x | match     |
| sha/n=32        |       3.55 |           2.35 |     1.51x | match     |
| proto           |      55.17 |          10.27 |     5.37x | match     |
+-----------------+------------+----------------+-----------+-----------+
```

### `--verilator`

With `--verilator`, each Codegen config also builds and runs the external Verilator model of that
top (from the committed `sandbox/<Top>/hdl`) and prints its measurement next to the DFacsimile
one. Verilator must be on `PATH` (`verilator` on Unix, `verilator_bin.exe` on Windows); override
the binary with `VERILATOR_BIN`. On Windows also set `VERILATOR_ROOT` (forward slashes, e.g.
`C:/oss-cad-suite/share/verilator`) since the generated makefile's back-slashed root is otherwise
stripped by the shell. A missing tool or failed build is reported and that top is skipped.

For peak DFacsimile throughput start the JVM with `--add-modules jdk.incubator.vector` (the
parent repo's `.jvmopts` already does this for sbt) — without it the codegen kernel falls back
to a scalar register commit.

## Verilator harnesses

Each work's `verilator/` folder holds the C++ main for the external comparison, verilated with
`--prefix VTOP` so one harness serves every top of that work. The `--verilator` flag above drives
this automatically; the manual invocation, from the committed Verilog under `sandbox/<Top>/hdl`
(produced by the benchmark mains above), is:

```
verilator -O3 --cc --prefix VTOP --exe -Mdir obj_dir --top-module SHAFarm64 \
  -Ihdl SHAFarm64.sv SHA256Unit.sv ../sha_farm/verilator/bench_sha.cpp
make -C obj_dir -f VTOP.mk VTOP OPT_FAST="-O3 -march=native"
obj_dir/VTOP <warmup_cycles> <timed_cycles>
```

Harnesses use the two-eval clock loop (posedge + negedge per cycle) and print the same state
line format as the DFacsimile mains.
