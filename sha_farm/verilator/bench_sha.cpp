// Verilator harness for the SHAFarm scaling benchmark. Verilate with --prefix VTOP so the same
// harness serves every farm size. Args: [warmup_cycles timed_cycles], default 1M/10M.
#include "VTOP.h"
#include "verilated.h"
#include <chrono>
#include <cstdio>
#include <cstdlib>

double sc_time_stamp() { return 0; }

int main(int argc, char** argv) {
    Verilated::commandArgs(argc, argv);
    unsigned long long WARM = 1000000ULL;
    unsigned long long N = 10000000ULL;
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
    printf("[Verilator] timed %llu cycles in %.3f s = %.3f Mcycles/s\n",
           N, secs, N / secs / 1e6);
    printf("  after %llu cycles: agg=%08x%08x%08x%08x\n",
           WARM + N, top->agg[3], top->agg[2], top->agg[1], top->agg[0]);
    top->final();
    delete top;
    return 0;
}
