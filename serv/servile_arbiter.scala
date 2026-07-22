// DFHDL port of servile_arbiter.v from Servile, the SERV convenience wrapper by Olof Kindgren
// (see serv_top.scala for the port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2024 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.serv

import dfhdl.*

/** servile_arbiter.v: I/D bus arbiter (relies on ibus and dbus never being active at the same
  * time).
  */
class servile_arbiter extends RTDesign:
  val dbus_adr = Bits(32) <> IN
  val dbus_dat = Bits(32) <> IN
  val dbus_sel = Bits(4) <> IN
  val dbus_we = Bit <> IN
  val dbus_stb = Bit <> IN
  val dbus_rdt = Bits(32) <> OUT
  val dbus_ack = Bit <> OUT
  val ibus_adr = Bits(32) <> IN
  val ibus_stb = Bit <> IN
  val ibus_rdt = Bits(32) <> OUT
  val ibus_ack = Bit <> OUT
  val mem_adr = Bits(32) <> OUT
  val mem_dat = Bits(32) <> OUT
  val mem_sel = Bits(4) <> OUT
  val mem_we = Bit <> OUT
  val mem_stb = Bit <> OUT
  val mem_rdt = Bits(32) <> IN
  val mem_ack = Bit <> IN

  dbus_rdt := mem_rdt
  dbus_ack := mem_ack && !ibus_stb
  ibus_rdt := mem_rdt
  ibus_ack := mem_ack && ibus_stb
  mem_adr := ibus_stb.sel(ibus_adr, dbus_adr)
  mem_dat := dbus_dat
  mem_sel := dbus_sel
  mem_we := dbus_we && !ibus_stb
  mem_stb := ibus_stb || dbus_stb
end servile_arbiter
