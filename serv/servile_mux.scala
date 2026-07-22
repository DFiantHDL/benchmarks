// DFHDL port of servile_mux.v from Servile, the SERV convenience wrapper by Olof Kindgren
// (see serv_top.scala for the port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2024 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.serv

import dfhdl.*

/** servile_mux.v: data-bus mux between memory (low address space) and the external bus, with the
  * simulation halt-address swallow (sim = 1, no signature file).
  */
class servile_mux extends RTDesign:
  val wb_rst = Bit <> IN
  val cpu_adr = Bits(32) <> IN
  val cpu_dat = Bits(32) <> IN
  val cpu_sel = Bits(4) <> IN
  val cpu_we = Bit <> IN
  val cpu_stb = Bit <> IN
  val cpu_rdt = Bits(32) <> OUT
  val cpu_ack = Bit <> OUT
  val mem_adr = Bits(32) <> OUT
  val mem_dat = Bits(32) <> OUT
  val mem_sel = Bits(4) <> OUT
  val mem_we = Bit <> OUT
  val mem_stb = Bit <> OUT
  val mem_rdt = Bits(32) <> IN
  val mem_ack = Bit <> IN
  val ext_adr = Bits(32) <> OUT
  val ext_dat = Bits(32) <> OUT
  val ext_sel = Bits(4) <> OUT
  val ext_we = Bit <> OUT
  val ext_stb = Bit <> OUT
  val ext_rdt = Bits(32) <> IN
  val ext_ack = Bit <> IN
  val halt = Bit <> OUT

  val sim_ack = Bit <> VAR.REG init 0
  val ext = cpu_adr(31, 30) != b"00"
  val halt_en = cpu_we && (cpu_adr == h"90000000")
  cpu_rdt := ext.sel(ext_rdt, mem_rdt)
  cpu_ack := ext_ack || mem_ack || sim_ack
  mem_adr := cpu_adr
  mem_dat := cpu_dat
  mem_sel := cpu_sel
  mem_we := cpu_we
  mem_stb := cpu_stb && !ext && !halt_en
  ext_adr := cpu_adr
  ext_dat := cpu_dat
  ext_sel := cpu_sel
  ext_we := cpu_we
  ext_stb := cpu_stb && ext && !halt_en
  sim_ack.din := 0
  if (cpu_stb && !sim_ack) sim_ack.din := halt_en
  if (wb_rst) sim_ack.din := 0
  halt := sim_ack
end servile_mux
