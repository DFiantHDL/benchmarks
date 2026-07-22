// DFHDL port of servile_mux.v from Servile, the SERV convenience wrapper by Olof Kindgren
// (see serv_top.scala for the port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2024 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.serv

import dfhdl.*

/** servile_mux.v: data-bus mux between memory (low address space) and the external bus, with the
  * simulation halt-address swallow (sim = 1). The baseline signals completion with `$finish`,
  * which is not synthesizable, so this port exposes an extra `o_halt` observation output instead;
  * the signature-file path (`sim_sig_adr`) is not ported. Clocked on `i_clk`, reset `i_rst`.
  */
@hw.constraints.timing.clock(portName = "i_clk")
@hw.constraints.timing.reset(portName = "i_rst")
class servile_mux extends RTDesign:
  val i_wb_cpu_adr = Bits(32) <> IN
  val i_wb_cpu_dat = Bits(32) <> IN
  val i_wb_cpu_sel = Bits(4) <> IN
  val i_wb_cpu_we = Bit <> IN
  val i_wb_cpu_stb = Bit <> IN
  val o_wb_cpu_rdt = Bits(32) <> OUT
  val o_wb_cpu_ack = Bit <> OUT
  val o_wb_mem_adr = Bits(32) <> OUT
  val o_wb_mem_dat = Bits(32) <> OUT
  val o_wb_mem_sel = Bits(4) <> OUT
  val o_wb_mem_we = Bit <> OUT
  val o_wb_mem_stb = Bit <> OUT
  val i_wb_mem_rdt = Bits(32) <> IN
  val i_wb_mem_ack = Bit <> IN
  val o_wb_ext_adr = Bits(32) <> OUT
  val o_wb_ext_dat = Bits(32) <> OUT
  val o_wb_ext_sel = Bits(4) <> OUT
  val o_wb_ext_we = Bit <> OUT
  val o_wb_ext_stb = Bit <> OUT
  val i_wb_ext_rdt = Bits(32) <> IN
  val i_wb_ext_ack = Bit <> IN
  val o_halt = Bit <> OUT // benchmark-added: pulses on the sim halt-address write

  val sim_ack = Bit <> VAR.REG init 0
  val ext = i_wb_cpu_adr(31, 30) != b"00"
  val halt_en = i_wb_cpu_we && (i_wb_cpu_adr == h"90000000")
  o_wb_cpu_rdt := ext.sel(i_wb_ext_rdt, i_wb_mem_rdt)
  o_wb_cpu_ack := i_wb_ext_ack || i_wb_mem_ack || sim_ack
  o_wb_mem_adr := i_wb_cpu_adr
  o_wb_mem_dat := i_wb_cpu_dat
  o_wb_mem_sel := i_wb_cpu_sel
  o_wb_mem_we := i_wb_cpu_we
  o_wb_mem_stb := i_wb_cpu_stb && !ext && !halt_en
  o_wb_ext_adr := i_wb_cpu_adr
  o_wb_ext_dat := i_wb_cpu_dat
  o_wb_ext_sel := i_wb_cpu_sel
  o_wb_ext_we := i_wb_cpu_we
  o_wb_ext_stb := i_wb_cpu_stb && ext && !halt_en
  sim_ack.din := 0
  if (i_wb_cpu_stb && !sim_ack) sim_ack.din := halt_en
  o_halt := sim_ack
end servile_mux
