// DFHDL port of servant.v from the Servant SoC by Olof Kindgren (see serv_top.scala for the
// port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2019 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** servant.v: the Servant SoC. `memsize` is the RAM size in bytes (a power of two). Clocked on the
  * package-default `wb_clk`/`wb_rst`. Besides the baseline `q` (GPIO) output, this port exposes the
  * benchmark-added `halt`, `pc_adr` and `pc_vld` observation outputs for the simulation harness.
  */
class servant(
    val memfile: String = "benchmarks/serv/sw/hello_uart.hex",
    val memsize: Int <> CONST = 8192
) extends RTDesign:
  private def log2(n: Int): Int = 31 - Integer.numberOfLeadingZeros(n)
  val q = Bit <> OUT
  val halt = Bit <> OUT // benchmark-added
  val pc_adr = Bits(32) <> OUT // benchmark-added
  val pc_vld = Bit <> OUT // benchmark-added

  val cpu = servile()
  val mux = servant_mux()
  val ram = servant_ram(memfile, memsize)
  val timer = servant_timer()
  val gpio = servant_gpio()
  val rf_ram = serv_rf_ram()
  cpu.i_timer_irq <> timer.o_irq

  ram.i_wb_adr <> cpu.o_wb_mem_adr(log2(memsize.toScalaInt) - 1, 2)
  ram.i_wb_dat <> cpu.o_wb_mem_dat
  ram.i_wb_sel <> cpu.o_wb_mem_sel
  ram.i_wb_we <> cpu.o_wb_mem_we
  ram.i_wb_cyc <> cpu.o_wb_mem_stb
  cpu.i_wb_mem_rdt <> ram.o_wb_rdt
  cpu.i_wb_mem_ack <> ram.o_wb_ack

  mux.i_wb_cpu_adr <> cpu.o_wb_ext_adr
  mux.i_wb_cpu_dat <> cpu.o_wb_ext_dat
  mux.i_wb_cpu_sel <> cpu.o_wb_ext_sel
  mux.i_wb_cpu_we <> cpu.o_wb_ext_we
  mux.i_wb_cpu_cyc <> cpu.o_wb_ext_stb
  cpu.i_wb_ext_rdt <> mux.o_wb_cpu_rdt
  cpu.i_wb_ext_ack <> mux.o_wb_cpu_ack

  gpio.i_wb_dat <> mux.o_wb_gpio_dat
  gpio.i_wb_we <> mux.o_wb_gpio_we
  gpio.i_wb_cyc <> mux.o_wb_gpio_cyc
  mux.i_wb_gpio_rdt <> gpio.o_wb_rdt
  q <> gpio.o_gpio

  timer.i_wb_dat <> mux.o_wb_timer_dat
  timer.i_wb_we <> mux.o_wb_timer_we
  timer.i_wb_cyc <> mux.o_wb_timer_cyc
  mux.i_wb_timer_rdt <> timer.o_wb_dat

  rf_ram.i_waddr <> cpu.o_rf_waddr
  rf_ram.i_wdata <> cpu.o_rf_wdata
  rf_ram.i_wen <> cpu.o_rf_wen
  rf_ram.i_raddr <> cpu.o_rf_raddr
  rf_ram.i_ren <> cpu.o_rf_ren
  cpu.i_rf_rdata <> rf_ram.o_rdata

  halt <> cpu.o_halt
  pc_adr <> cpu.o_wb_mem_adr
  pc_vld <> ram.o_wb_ack
end servant
