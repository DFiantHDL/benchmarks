// SPDX-License-Identifier: Apache-2.0
// Verilator harness for the ProtocolEngine benchmark. Verilate with --prefix VTOP.
// Args: [warmup_cycles timed_cycles], default 2M/100M (matching the DFacsimile side).
#include "VTOP.h"
#include "verilated.h"
#include <chrono>
#include <cstdio>
#include <cstdlib>

double sc_time_stamp() { return 0; }

int main(int argc, char** argv) {
    Verilated::commandArgs(argc, argv);
    unsigned long long WARM = 2000000ULL;
    unsigned long long N = 100000000ULL;
    if (argc > 2) {
        WARM = strtoull(argv[1], nullptr, 10);
        N = strtoull(argv[2], nullptr, 10);
    }
    VTOP* top = new VTOP;

    top->rst = 1;
    top->clk = 0; top->eval();
    top->clk = 1; top->eval();
    top->clk = 0; top->eval();
    top->rst = 0;

    for (unsigned long long i = 0; i < WARM; ++i) {
        top->clk = 1; top->eval();
        top->clk = 0; top->eval();
    }
    auto t0 = std::chrono::steady_clock::now();
    for (unsigned long long i = 0; i < N; ++i) {
        top->clk = 1; top->eval();
        top->clk = 0; top->eval();
    }
    auto t1 = std::chrono::steady_clock::now();
    double secs = std::chrono::duration<double>(t1 - t0).count();
    printf("[Verilator] timed %llu cycles in %.3f s = %.2f Mcycles/s\n",
           N, secs, N / secs / 1e6);
    printf("  after %llu cycles: packets=%u drops=%u sig=%08x trail=%08x%08x%08x%08x "
           "phase=%u beat=%u\n",
           WARM + N, top->packets, top->drops, top->sig,
           top->trail[3], top->trail[2], top->trail[1], top->trail[0],
           (unsigned) top->phase, (unsigned) top->beat);
    top->final();
    delete top;
    return 0;
}
