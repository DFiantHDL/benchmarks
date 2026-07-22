// DFHDL port of servant_mux.v from the Servant SoC by Olof Kindgren (see serv_top.scala
// for the port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2019 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** servant_mux.v: external-bus mux between GPIO (address bit 30 clear) and timer (bit 30 set).
  * Clocked on `i_clk` with the synchronous MINI reset `i_rst`.
  */
@hw.constraints.timing.clock(portName = "i_clk")
@hw.constraints.timing.reset(portName = "i_rst")
class servant_mux extends RTDesign:
  val i_wb_cpu_adr = Bits(32) <> IN
  val i_wb_cpu_dat = Bits(32) <> IN
  val i_wb_cpu_sel = Bits(4) <> IN
  val i_wb_cpu_we = Bit <> IN
  val i_wb_cpu_cyc = Bit <> IN
  val o_wb_cpu_rdt = Bits(32) <> OUT
  val o_wb_cpu_ack = Bit <> OUT.REG init 0
  val o_wb_gpio_dat = Bit <> OUT
  val o_wb_gpio_we = Bit <> OUT
  val o_wb_gpio_cyc = Bit <> OUT
  val i_wb_gpio_rdt = Bit <> IN
  val o_wb_timer_dat = Bits(32) <> OUT
  val o_wb_timer_we = Bit <> OUT
  val o_wb_timer_cyc = Bit <> OUT
  val i_wb_timer_rdt = Bits(32) <> IN

  val s = i_wb_cpu_adr(31, 30)
  o_wb_cpu_rdt := s(1).sel(i_wb_timer_rdt, i_wb_gpio_rdt.toBits(32))
  o_wb_cpu_ack.din := 0
  if (i_wb_cpu_cyc && !o_wb_cpu_ack) o_wb_cpu_ack.din := 1
  o_wb_gpio_dat := i_wb_cpu_dat(0)
  o_wb_gpio_we := i_wb_cpu_we
  o_wb_gpio_cyc := i_wb_cpu_cyc && !s(1)
  o_wb_timer_dat := i_wb_cpu_dat
  o_wb_timer_we := i_wb_cpu_we
  o_wb_timer_cyc := i_wb_cpu_cyc && s(1)
end servant_mux
