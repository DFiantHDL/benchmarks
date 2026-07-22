// DFHDL port of servant.v from the Servant SoC by Olof Kindgren (see serv_top.scala
// for the port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2019 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** servant.v: the Servant SoC. `memsize` is the RAM size in bytes (a power of two). */
class servant(val memfile: String, val memsize: Int = 32768) extends RTDesign:
  private def log2(n: Int): Int = 31 - Integer.numberOfLeadingZeros(n)
  val wb_rst = Bit <> IN
  val q = Bit <> OUT
  val halt = Bit <> OUT
  val pc_adr = Bits(32) <> OUT
  val pc_vld = Bit <> OUT

  val cpu = servile()
  val mux = servant_mux()
  val ram = servant_ram(memfile, memsize / 4)
  val timer = servant_timer()
  val gpio = servant_gpio()
  val rf_ram = serv_rf_ram()
  cpu.wb_rst <> wb_rst
  cpu.timer_irq <> timer.irq

  ram.wb_rst <> wb_rst
  ram.wb_adr <> cpu.wb_mem_adr(log2(memsize) - 1, 2)
  ram.wb_dat <> cpu.wb_mem_dat
  ram.wb_sel <> cpu.wb_mem_sel
  ram.wb_we <> cpu.wb_mem_we
  ram.wb_cyc <> cpu.wb_mem_stb
  cpu.wb_mem_rdt <> ram.rdt
  cpu.wb_mem_ack <> ram.ack

  mux.wb_rst <> wb_rst
  mux.cpu_adr <> cpu.wb_ext_adr
  mux.cpu_dat <> cpu.wb_ext_dat
  mux.cpu_we <> cpu.wb_ext_we
  mux.cpu_cyc <> cpu.wb_ext_stb
  cpu.wb_ext_rdt <> mux.cpu_rdt
  cpu.wb_ext_ack <> mux.cpu_ack

  gpio.wb_dat <> mux.gpio_dat
  gpio.wb_we <> mux.gpio_we
  gpio.wb_cyc <> mux.gpio_cyc
  mux.gpio_rdt <> gpio.wb_rdt
  q <> gpio.gpio

  timer.wb_rst <> wb_rst
  timer.wb_dat <> mux.timer_dat
  timer.wb_we <> mux.timer_we
  timer.wb_cyc <> mux.timer_cyc
  mux.timer_rdt <> timer.wb_rdt

  rf_ram.waddr <> cpu.rf_waddr
  rf_ram.wdata <> cpu.rf_wdata
  rf_ram.wen <> cpu.rf_wen
  rf_ram.raddr <> cpu.rf_raddr
  rf_ram.ren <> cpu.rf_ren
  cpu.rf_rdata <> rf_ram.rdata

  halt <> cpu.halt
  pc_adr <> cpu.wb_mem_adr
  pc_vld <> ram.ack
end servant
