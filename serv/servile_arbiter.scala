// DFHDL port of servile_arbiter.v from Servile, the SERV convenience wrapper by Olof Kindgren
// (see serv_top.scala for the port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2024 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.serv

import dfhdl.*

/** servile_arbiter.v: I/D bus arbiter (relies on ibus and dbus never being active at the same
  * time). Purely combinational, so it carries no clock or reset.
  */
class servile_arbiter extends RTDesign:
  val i_wb_cpu_dbus_adr = Bits(32) <> IN
  val i_wb_cpu_dbus_dat = Bits(32) <> IN
  val i_wb_cpu_dbus_sel = Bits(4) <> IN
  val i_wb_cpu_dbus_we = Bit <> IN
  val i_wb_cpu_dbus_stb = Bit <> IN
  val o_wb_cpu_dbus_rdt = Bits(32) <> OUT
  val o_wb_cpu_dbus_ack = Bit <> OUT
  val i_wb_cpu_ibus_adr = Bits(32) <> IN
  val i_wb_cpu_ibus_stb = Bit <> IN
  val o_wb_cpu_ibus_rdt = Bits(32) <> OUT
  val o_wb_cpu_ibus_ack = Bit <> OUT
  val o_wb_mem_adr = Bits(32) <> OUT
  val o_wb_mem_dat = Bits(32) <> OUT
  val o_wb_mem_sel = Bits(4) <> OUT
  val o_wb_mem_we = Bit <> OUT
  val o_wb_mem_stb = Bit <> OUT
  val i_wb_mem_rdt = Bits(32) <> IN
  val i_wb_mem_ack = Bit <> IN

  o_wb_cpu_dbus_rdt := i_wb_mem_rdt
  o_wb_cpu_dbus_ack := i_wb_mem_ack && !i_wb_cpu_ibus_stb
  o_wb_cpu_ibus_rdt := i_wb_mem_rdt
  o_wb_cpu_ibus_ack := i_wb_mem_ack && i_wb_cpu_ibus_stb
  o_wb_mem_adr := i_wb_cpu_ibus_stb.sel(i_wb_cpu_ibus_adr, i_wb_cpu_dbus_adr)
  o_wb_mem_dat := i_wb_cpu_dbus_dat
  o_wb_mem_sel := i_wb_cpu_dbus_sel
  o_wb_mem_we := i_wb_cpu_dbus_we && !i_wb_cpu_ibus_stb
  o_wb_mem_stb := i_wb_cpu_ibus_stb || i_wb_cpu_dbus_stb
end servile_arbiter
