// Benchmark top mirroring the baseline's servant_sim.v observability (plus the UART
// monitor and halt/memory-ack counters). Written fresh for the DFHDL benchmarks repository.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** servant_sim.v equivalent: the servant SoC with the UART monitor and halt/memory-ack counters.
  * All monitor state is exported as output ports, so a fixed cycle count yields one comparable
  * state line in every simulator.
  */
class servant_sim(val memfile: String, val memsize: Int = 32768) extends RTDesign:
  val wb_rst = Bit <> IN
  val q = Bit <> OUT
  val pc_adr = Bits(32) <> OUT
  val pc_vld = Bit <> OUT
  val char_count = UInt(32) <> OUT
  val line_count = UInt(32) <> OUT
  val char_sig = Bits(32) <> OUT
  val last_char = Bits(8) <> OUT
  val halt_count = UInt(32) <> OUT.REG init 0
  val mem_ack_count = UInt(32) <> OUT.REG init 0

  val soc = new servant(memfile, memsize)
  val mon = new uart_decoder()

  soc.wb_rst <> wb_rst
  q <> soc.q
  pc_adr <> soc.pc_adr
  pc_vld <> soc.pc_vld
  mon.rx <> soc.q
  char_count <> mon.char_count
  line_count <> mon.line_count
  char_sig <> mon.char_sig
  last_char <> mon.last_char
  if (soc.halt) halt_count.din := halt_count + 1
  if (soc.pc_vld) mem_ack_count.din := mem_ack_count + 1
end servant_sim
