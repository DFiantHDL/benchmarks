// DFHDL port of servant_mux.v from the Servant SoC by Olof Kindgren (see serv_top.scala
// for the port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2019 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** servant_mux.v: external-bus mux between GPIO (address bit 30 clear) and timer (bit 30 set).
  */
class servant_mux extends RTDesign:
  val wb_rst = Bit <> IN
  val cpu_adr = Bits(32) <> IN
  val cpu_dat = Bits(32) <> IN
  val cpu_we = Bit <> IN
  val cpu_cyc = Bit <> IN
  val cpu_rdt = Bits(32) <> OUT
  val cpu_ack = Bit <> OUT.REG init 0
  val gpio_dat = Bit <> OUT
  val gpio_we = Bit <> OUT
  val gpio_cyc = Bit <> OUT
  val gpio_rdt = Bit <> IN
  val timer_dat = Bits(32) <> OUT
  val timer_we = Bit <> OUT
  val timer_cyc = Bit <> OUT
  val timer_rdt = Bits(32) <> IN

  val s = cpu_adr(31, 30)
  cpu_rdt := s(1).sel(timer_rdt, gpio_rdt.toBits(32))
  cpu_ack.din := 0
  if (cpu_cyc && !cpu_ack) cpu_ack.din := 1
  if (wb_rst) cpu_ack.din := 0
  gpio_dat := cpu_dat(0)
  gpio_we := cpu_we
  gpio_cyc := cpu_cyc && !s(1)
  timer_dat := cpu_dat
  timer_we := cpu_we
  timer_cyc := cpu_cyc && s(1)
end servant_mux
