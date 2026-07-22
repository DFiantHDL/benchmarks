# SERV / Servant

A DFHDL port of [SERV](https://github.com/olofk/serv), Olof Kindgren's award-winning
bit-serial RISC-V CPU, wrapped in the Servant SoC (firmware RAM, timer, GPIO) exactly as
benchmarked by Verilator's [RTLMeter](https://github.com/verilator/rtlmeter) suite. This is
the suite's first real-CPU-running-real-software benchmark: control-dominated, almost
entirely 1-bit signals, with a RAM-based register file, the opposite workload profile of
`sha_farm`'s wide datapath.

- **Origin**: https://github.com/olofk/serv @ `7d9cde4e6ca4f4c84d7512a752a4c187537dd373`,
  imported from the RTLMeter design copy (`designs/Servant`), including the two firmware
  images under [sw/](sw/)
- **License**: ISC (see [LICENSE](LICENSE)), except the three `servile*` files, which are
  Apache-2.0 like their originals; the benchmark additions in this folder are ISC
- **Layout**: each ported design keeps its baseline Verilog module name (case-sensitive) and
  lives in a file of the same name (`serv_top` in `serv_top.scala`, ...); the workload tops
  (`ServantHello`, `ServantPhil`, `ServantHelloMini`) are benchmark additions with no
  baseline counterpart
- **Run**: `benchmarks/runMain dfhdl.benchmarks.serv.servBench` (commit Verilog + DFacsimile
  bench) and `benchmarks/runMain dfhdl.benchmarks.serv.servText` (decode the hello UART text)
- **Verilator side**: `verilator/bench_serv.cpp`, one harness for every top (`--prefix VTOP`)

## Port configuration

The port fixes the parameters at RTLMeter's `serv` configuration: `W = 1` (bit-serial),
`WITH_CSR = 1`, `PRE_REGISTER = 1`, `RESET_STRATEGY = "MINI"`, `RESET_PC = 0`, no MDU,
compressed-instruction, or alignment support (the QERV `W = 4` variant is future work). The
`serv_debug` module (simulation introspection only, no architectural effect) is not ported.

Deliberate portable-behavior deviations, none architecturally observable:

- The original holds the reset combinationally in several places, so the architectural reset
  is a plain `wb_rst` input port (the name `rst` is reserved by the implicit reset magnet);
  the harness asserts it for exactly one cycle, like `servant_tb.v`.
- Every register carries a power-up init (zero unless the MINI reset says otherwise), where
  the original leaves most registers undefined until first write, so 2-state and 4-state
  simulators agree bit-for-bit. In the generated HDL these inits load synchronously on the
  implicit `rst` port, so external harnesses assert `rst` (together with `wb_rst`) for one
  uncounted preamble cycle before the counted `wb_rst` cycle; DFacsimile applies inits at
  time zero and needs no preamble.
- The RF read register holds its previous value when read-enable is low (original: X), and
  `servile_mux` keeps the halt-address swallow but drops the signature-file plumbing,
  exporting a `halt` pulse instead of calling `$finish`.

## Workload tops

| Top | Firmware | RAM | Notes |
|---|---|---|---|
| `ServantHello` | `sw/hello_uart.hex` | 32 KiB | prints "Hi, I'm Servant!", then loops writing the halt address |
| `ServantPhil` | `sw/zephyr_phil.hex` | 32 KiB | Zephyr dining philosophers: timer interrupts, CSRs, endless output |
| `ServantHelloMini` | `sw/hello_uart.hex` | 256 B | hello on a small RAM (the firmware touches only the bottom 96 bytes) |

The designs are closed (no inputs besides `rst`), so a fixed cycle count must yield an
identical state line in every simulator. The benchmark top adds an in-design UART monitor
(280 cycles per bit, the 57600-baud/16.129-MHz ratio of `servant_tb.v`) plus halt and
memory-ack counters, all exported as ports:

```
chars=<n> lines=<n> sig=<rotate-xor of all bytes> last=<byte> halts=<n> memacks=<n> pc=<adr>
```

## DFacsimile status

All three tops run on DFacsimile, on both tiers, with state lines verified bit-exact against
Verilator (`ServantHelloMini` is the cross-simulator anchor at 55,000 and 2,100,000 cycles;
`ServantHello`/`ServantPhil` match too). This is thanks to the simulator's **memory node** (a
`long[]`-backed RAM with an O(1) async read and synchronous write ports), which this benchmark
forced: a `Bits(W) X D <> VAR.REG` accessed only cell-wise now lowers to that node instead of a
wide register whose every dynamic index expanded into a barrel-shift network over the whole
value. Before, only the 256-byte mini ran (Codegen ~0.6 Mcycles/s vs Verilator ~6) and 8192 x 32
bits overflowed the codegen kernel's class limits; now the mini runs at ~6 Mcycles/s (about
Verilator's speed) and the 32 KiB tops at ~5.7, RAM size no longer mattering.

## Verilator flow

From the committed Verilog under `sandbox/<Top>/hdl` (produced by `servBench` above):

```
verilator -O3 --cc --prefix VTOP --exe -Mdir obj_dir -Wno-fatal --top-module ServantHello \
  -Ihdl hdl/*.sv ../../benchmarks/serv/verilator/bench_serv.cpp
make -C obj_dir -f VTOP.mk VTOP OPT_FAST="-O3 -march=native"
obj_dir/VTOP <warmup_cycles> <timed_cycles>
```

`-Wno-fatal` silences two benign warning classes: the intentionally unconnected
`wb_ext_sel` pin and latch-style comb helpers the backend emits for conditional register
assignments. On Windows with the OSS CAD Suite outside its environment scripts, invoke
`verilator_bin.exe` directly and build with `VM_PARALLEL_BUILDS=1` (both sidestep the
suite's perl wrapper).
