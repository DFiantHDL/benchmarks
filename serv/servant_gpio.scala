// DFHDL port of servant_gpio.v from the Servant SoC by Olof Kindgren (see serv_top.scala
// for the port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2019 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** servant_gpio.v: single-bit GPIO output (the software UART TX pin). Clocked on `i_wb_clk`, no
  * reset.
  */
@hw.constraints.timing.clock(portName = "i_wb_clk")
class servant_gpio extends RTDesign:
  val i_wb_dat = Bit <> IN
  val i_wb_we = Bit <> IN
  val i_wb_cyc = Bit <> IN
  val o_wb_rdt = Bit <> OUT.REG init 0
  val o_gpio = Bit <> OUT.REG init 0

  o_wb_rdt.din := o_gpio
  if (i_wb_cyc && i_wb_we) o_gpio.din := i_wb_dat
end servant_gpio
