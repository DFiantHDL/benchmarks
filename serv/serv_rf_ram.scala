// DFHDL port of serv_rf_ram.v from SERV by Olof Kindgren (see serv_top.scala for the
// port-wide configuration and conventions).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2019 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_rf_ram.v: the RF SRAM (576 x 2 bits) with the x0-reads-as-zero output gate. The original
  * leaves the read register undefined when the read enable is low; this port keeps the previous
  * value instead (never architecturally consumed), so all simulators agree.
  */
class serv_rf_ram extends RTDesign:
  val waddr = Bits(10) <> IN
  val wdata = Bits(2) <> IN
  val wen = Bit <> IN
  val raddr = Bits(10) <> IN
  val ren = Bit <> IN
  val rdata = Bits(2) <> OUT

  val memory = Bits(2) X 576 <> VAR.REG init all(all(0))
  val rdata_r = Bits(2) <> VAR.REG init all(0)
  val regzero = Bit <> VAR.REG init 0

  if (wen) memory(waddr.uint).din := wdata
  if (ren) rdata_r.din := memory(raddr.uint)
  regzero.din := !raddr(9, 4).|
  rdata := regzero.sel(b"00", rdata_r)
end serv_rf_ram
