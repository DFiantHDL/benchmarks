# ProtocolEngine

A long-running packet protocol engine: a high-level FSM benchmark (step defs, dynamic-bound
`while` payload loop, data-dependent gap waits, concurrent LFSR and heartbeat processes)
exercising the control-flow side of a simulator rather than raw datapath throughput. The design
is closed (no inputs), so a fixed cycle count must yield identical state in any simulator.

- **Origin**: written fresh for this repository
- **License**: Apache 2.0 (repository default)
- **Run**: `benchmarks/runMain dfhdl.benchmarks.protocol_engine.protocolEngineBench`
- **Verilator side**: `verilator/bench_protocol_engine.cpp`
