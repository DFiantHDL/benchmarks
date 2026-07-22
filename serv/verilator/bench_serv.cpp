// Verilator harness for the Servant benchmark tops (ServantHello / ServantPhil /
// ServantHelloMini), verilated with --prefix VTOP so one harness serves every top.
// Runs <warmup> + <timed> clock cycles (two evals per cycle) after a one-cycle reset and
// prints throughput plus the same architectural state line as the DFacsimile mains.
//
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: ISC
#include "VTOP.h"
#include "verilated.h"
#include <chrono>
#include <cstdint>
#include <cstdio>
#include <cstdlib>

double sc_time_stamp() { return 0; }

int main(int argc, char** argv) {
    Verilated::commandArgs(argc, argv);
    uint64_t warmup = argc > 1 ? strtoull(argv[1], nullptr, 0) : 100000;
    uint64_t timed = argc > 2 ? strtoull(argv[2], nullptr, 0) : 10000000;
    VTOP* top = new VTOP;

    auto cycles = [&](uint64_t n) {
        for (uint64_t i = 0; i < n; i++) {
            top->clk = 1;
            top->eval();
            top->clk = 0;
            top->eval();
        }
    };

    // uncounted preamble: assert the implicit `rst` for one cycle so the generated Verilog
    // loads its register/memory power-up inits (DFacsimile applies these at time zero)
    top->clk = 0;
    top->rst = 1;
    top->wb_rst = 1;
    top->eval();
    cycles(1);
    top->rst = 0;
    // cycle 1: the architectural (wb_rst) reset cycle, matching the DFacsimile harness
    cycles(1);
    top->wb_rst = 0;
    cycles(warmup);
    auto t0 = std::chrono::steady_clock::now();
    cycles(timed);
    auto t1 = std::chrono::steady_clock::now();
    double dt = std::chrono::duration<double>(t1 - t0).count();
    uint64_t total = 1 + warmup + timed;
    printf("[verilator] timed %llu cycles in %.3f s = %.3f Mcycles/s\n",
           (unsigned long long)timed, dt, timed / dt / 1e6);
    printf("  after %llu cycles: chars=%u lines=%u sig=%08x last=%02x halts=%u memacks=%u "
           "pc=%08x\n",
           (unsigned long long)total, (unsigned)top->char_count, (unsigned)top->line_count,
           (unsigned)top->char_sig, (unsigned)top->last_char, (unsigned)top->halt_count,
           (unsigned)top->mem_ack_count, (unsigned)top->pc_adr);
    delete top;
    return 0;
}
