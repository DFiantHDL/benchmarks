// DFHDL port of servile.v from Servile, the SERV convenience wrapper by Olof Kindgren
// (see serv_top.scala for the port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2024 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.serv

import dfhdl.*

/** servile.v: SERV core + bus mux/arbiter + RF SRAM interface (the RF SRAM itself lives one level
  * up, in [[servant]]).
  */
class servile extends RTDesign:
  val wb_rst = Bit <> IN
  val timer_irq = Bit <> IN
  // memory (WB) interface
  val wb_mem_adr = Bits(32) <> OUT
  val wb_mem_dat = Bits(32) <> OUT
  val wb_mem_sel = Bits(4) <> OUT
  val wb_mem_we = Bit <> OUT
  val wb_mem_stb = Bit <> OUT
  val wb_mem_rdt = Bits(32) <> IN
  val wb_mem_ack = Bit <> IN
  // extension (WB) interface
  val wb_ext_adr = Bits(32) <> OUT
  val wb_ext_dat = Bits(32) <> OUT
  val wb_ext_sel = Bits(4) <> OUT
  val wb_ext_we = Bit <> OUT
  val wb_ext_stb = Bit <> OUT
  val wb_ext_rdt = Bits(32) <> IN
  val wb_ext_ack = Bit <> IN
  // RF (SRAM) interface
  val rf_waddr = Bits(10) <> OUT
  val rf_wdata = Bits(2) <> OUT
  val rf_wen = Bit <> OUT
  val rf_raddr = Bits(10) <> OUT
  val rf_ren = Bit <> OUT
  val rf_rdata = Bits(2) <> IN
  val halt = Bit <> OUT

  val cpu = serv_top()
  val mux = servile_mux()
  val arbiter = servile_arbiter()
  val rf_ram_if = serv_rf_ram_if()
  cpu.wb_rst <> wb_rst
  cpu.timer_irq <> timer_irq

  mux.wb_rst <> wb_rst
  mux.cpu_adr <> cpu.dbus_adr
  mux.cpu_dat <> cpu.dbus_dat
  mux.cpu_sel <> cpu.dbus_sel
  mux.cpu_we <> cpu.dbus_we
  mux.cpu_stb <> cpu.dbus_cyc
  cpu.dbus_rdt <> mux.cpu_rdt
  cpu.dbus_ack <> mux.cpu_ack
  wb_ext_adr <> mux.ext_adr
  wb_ext_dat <> mux.ext_dat
  wb_ext_sel <> mux.ext_sel
  wb_ext_we <> mux.ext_we
  wb_ext_stb <> mux.ext_stb
  mux.ext_rdt <> wb_ext_rdt
  mux.ext_ack <> wb_ext_ack
  halt <> mux.halt

  arbiter.dbus_adr <> mux.mem_adr
  arbiter.dbus_dat <> mux.mem_dat
  arbiter.dbus_sel <> mux.mem_sel
  arbiter.dbus_we <> mux.mem_we
  arbiter.dbus_stb <> mux.mem_stb
  mux.mem_rdt <> arbiter.dbus_rdt
  mux.mem_ack <> arbiter.dbus_ack
  arbiter.ibus_adr <> cpu.ibus_adr
  arbiter.ibus_stb <> cpu.ibus_cyc
  cpu.ibus_rdt <> arbiter.ibus_rdt
  cpu.ibus_ack <> arbiter.ibus_ack
  wb_mem_adr <> arbiter.mem_adr
  wb_mem_dat <> arbiter.mem_dat
  wb_mem_sel <> arbiter.mem_sel
  wb_mem_we <> arbiter.mem_we
  wb_mem_stb <> arbiter.mem_stb
  arbiter.mem_rdt <> wb_mem_rdt
  arbiter.mem_ack <> wb_mem_ack

  rf_ram_if.wb_rst <> wb_rst
  rf_ram_if.wreq <> cpu.rf_wreq
  rf_ram_if.rreq <> cpu.rf_rreq
  cpu.rf_ready <> rf_ram_if.ready
  rf_ram_if.wreg0 <> cpu.wreg0
  rf_ram_if.wreg1 <> cpu.wreg1
  rf_ram_if.wen0 <> cpu.wen0
  rf_ram_if.wen1 <> cpu.wen1
  rf_ram_if.wdata0 <> cpu.wdata0
  rf_ram_if.wdata1 <> cpu.wdata1
  rf_ram_if.rreg0 <> cpu.rreg0
  rf_ram_if.rreg1 <> cpu.rreg1
  cpu.rdata0 <> rf_ram_if.rdata0
  cpu.rdata1 <> rf_ram_if.rdata1
  rf_waddr <> rf_ram_if.waddr
  rf_wdata <> rf_ram_if.wdata
  rf_wen <> rf_ram_if.wen
  rf_raddr <> rf_ram_if.raddr
  rf_ren <> rf_ram_if.ren
  rf_ram_if.ram_rdata <> rf_rdata
end servile
