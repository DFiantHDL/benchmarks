// DFHDL port of servant_gpio.v from the Servant SoC by Olof Kindgren (see serv_top.scala
// for the port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2019 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** servant_gpio.v: single-bit GPIO output (the software UART TX pin). */
class servant_gpio extends RTDesign:
  val wb_dat = Bit <> IN
  val wb_we = Bit <> IN
  val wb_cyc = Bit <> IN
  val wb_rdt = Bit <> OUT.REG init 0
  val gpio = Bit <> OUT.REG init 0

  wb_rdt.din := gpio
  if (wb_cyc && wb_we) gpio.din := wb_dat
end servant_gpio
