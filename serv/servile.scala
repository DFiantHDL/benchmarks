// DFHDL port of servile.v from Servile, the SERV convenience wrapper by Olof Kindgren
// (see serv_top.scala for the port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2024 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.serv

import dfhdl.*

/** servile.v: SERV core + bus mux/arbiter + RF SRAM interface (the RF SRAM itself lives one level
  * up, in [[servant]]). Clocked on `i_clk` with the synchronous MINI reset `i_rst`. `o_halt` is a
  * benchmark-added observation output forwarded up from [[servile_mux]].
  */
@hw.constraints.timing.clock(portName = "i_clk")
@hw.constraints.timing.reset(portName = "i_rst")
class servile extends RTDesign:
  val i_timer_irq = Bit <> IN
  // memory (WB) interface
  val o_wb_mem_adr = Bits(32) <> OUT
  val o_wb_mem_dat = Bits(32) <> OUT
  val o_wb_mem_sel = Bits(4) <> OUT
  val o_wb_mem_we = Bit <> OUT
  val o_wb_mem_stb = Bit <> OUT
  val i_wb_mem_rdt = Bits(32) <> IN
  val i_wb_mem_ack = Bit <> IN
  // extension (WB) interface
  val o_wb_ext_adr = Bits(32) <> OUT
  val o_wb_ext_dat = Bits(32) <> OUT
  val o_wb_ext_sel = Bits(4) <> OUT
  val o_wb_ext_we = Bit <> OUT
  val o_wb_ext_stb = Bit <> OUT
  val i_wb_ext_rdt = Bits(32) <> IN
  val i_wb_ext_ack = Bit <> IN
  // RF (SRAM) interface
  val o_rf_waddr = Bits(10) <> OUT
  val o_rf_wdata = Bits(2) <> OUT
  val o_rf_wen = Bit <> OUT
  val o_rf_raddr = Bits(10) <> OUT
  val i_rf_rdata = Bits(2) <> IN
  val o_rf_ren = Bit <> OUT
  val o_halt = Bit <> OUT // benchmark-added

  val cpu = serv_top()
  val mux = servile_mux()
  val arbiter = servile_arbiter()
  val rf_ram_if = serv_rf_ram_if()
  cpu.i_timer_irq <> i_timer_irq

  mux.i_wb_cpu_adr <> cpu.o_dbus_adr
  mux.i_wb_cpu_dat <> cpu.o_dbus_dat
  mux.i_wb_cpu_sel <> cpu.o_dbus_sel
  mux.i_wb_cpu_we <> cpu.o_dbus_we
  mux.i_wb_cpu_stb <> cpu.o_dbus_cyc
  cpu.i_dbus_rdt <> mux.o_wb_cpu_rdt
  cpu.i_dbus_ack <> mux.o_wb_cpu_ack
  o_wb_ext_adr <> mux.o_wb_ext_adr
  o_wb_ext_dat <> mux.o_wb_ext_dat
  o_wb_ext_sel <> mux.o_wb_ext_sel
  o_wb_ext_we <> mux.o_wb_ext_we
  o_wb_ext_stb <> mux.o_wb_ext_stb
  mux.i_wb_ext_rdt <> i_wb_ext_rdt
  mux.i_wb_ext_ack <> i_wb_ext_ack
  o_halt <> mux.o_halt

  arbiter.i_wb_cpu_dbus_adr <> mux.o_wb_mem_adr
  arbiter.i_wb_cpu_dbus_dat <> mux.o_wb_mem_dat
  arbiter.i_wb_cpu_dbus_sel <> mux.o_wb_mem_sel
  arbiter.i_wb_cpu_dbus_we <> mux.o_wb_mem_we
  arbiter.i_wb_cpu_dbus_stb <> mux.o_wb_mem_stb
  mux.i_wb_mem_rdt <> arbiter.o_wb_cpu_dbus_rdt
  mux.i_wb_mem_ack <> arbiter.o_wb_cpu_dbus_ack
  arbiter.i_wb_cpu_ibus_adr <> cpu.o_ibus_adr
  arbiter.i_wb_cpu_ibus_stb <> cpu.o_ibus_cyc
  cpu.i_ibus_rdt <> arbiter.o_wb_cpu_ibus_rdt
  cpu.i_ibus_ack <> arbiter.o_wb_cpu_ibus_ack
  o_wb_mem_adr <> arbiter.o_wb_mem_adr
  o_wb_mem_dat <> arbiter.o_wb_mem_dat
  o_wb_mem_sel <> arbiter.o_wb_mem_sel
  o_wb_mem_we <> arbiter.o_wb_mem_we
  o_wb_mem_stb <> arbiter.o_wb_mem_stb
  arbiter.i_wb_mem_rdt <> i_wb_mem_rdt
  arbiter.i_wb_mem_ack <> i_wb_mem_ack

  rf_ram_if.i_wreq <> cpu.o_rf_wreq
  rf_ram_if.i_rreq <> cpu.o_rf_rreq
  cpu.i_rf_ready <> rf_ram_if.o_ready
  rf_ram_if.i_wreg0 <> cpu.o_wreg0
  rf_ram_if.i_wreg1 <> cpu.o_wreg1
  rf_ram_if.i_wen0 <> cpu.o_wen0
  rf_ram_if.i_wen1 <> cpu.o_wen1
  rf_ram_if.i_wdata0 <> cpu.o_wdata0
  rf_ram_if.i_wdata1 <> cpu.o_wdata1
  rf_ram_if.i_rreg0 <> cpu.o_rreg0
  rf_ram_if.i_rreg1 <> cpu.o_rreg1
  cpu.i_rdata0 <> rf_ram_if.o_rdata0
  cpu.i_rdata1 <> rf_ram_if.o_rdata1
  o_rf_waddr <> rf_ram_if.o_waddr
  o_rf_wdata <> rf_ram_if.o_wdata
  o_rf_wen <> rf_ram_if.o_wen
  o_rf_raddr <> rf_ram_if.o_raddr
  o_rf_ren <> rf_ram_if.o_ren
  rf_ram_if.i_rdata <> i_rf_rdata
end servile
