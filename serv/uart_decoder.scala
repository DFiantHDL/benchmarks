// Benchmark stand-in for the baseline's uart_decoder.v: a synthesizable, cycle-counting
// 8N1 UART RX monitor (the original is a simulation-time decoder).
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** A cycle-counting 8N1 UART RX monitor on the SoC's software-UART pin. It arms only after seeing
  * the line idle high (mirroring the original decoder's negedge trigger), resyncs on every start
  * bit, and folds each received byte into a rotate-xor signature.
  */
@hw.constraints.timing.clock(portName = "wb_clk")
class uart_decoder(val cyclesPerBit: Int <> CONST = 280) extends RTDesign:
  val rx = Bit <> IN
  val char_count = UInt(32) <> OUT.REG init 0
  val line_count = UInt(32) <> OUT.REG init 0
  val char_sig = Bits(32) <> OUT.REG init all(0)
  val last_char = Bits(8) <> OUT.REG init all(0)

  val armed = Bit <> VAR.REG init 0
  val busy = Bit <> VAR.REG init 0
  val cnt = UInt(16) <> VAR.REG init 0
  val bit_idx = UInt(4) <> VAR.REG init 0
  val shreg = Bits(8) <> VAR.REG init all(0)

  if (!busy)
    if (rx) armed.din := 1
    if (armed && !rx) // start-bit falling edge
      busy.din := 1
      bit_idx.din := 0
      cnt.din := cyclesPerBit / 2
  else if (cnt == 0)
    if (bit_idx == 0) // middle of the start bit
      if (rx) busy.din := 0 // false start
      else
        bit_idx.din := 1
        cnt.din := cyclesPerBit - 1
    else if (bit_idx <= 8) // data bits, LSB first
      val ch = (rx, shreg(7, 1)).toBits
      shreg.din := ch
      if (bit_idx == 8) // last data bit: commit the byte
        char_count.din := char_count + 1
        last_char.din := ch
        char_sig.din := (char_sig(26, 0), char_sig(31, 27)).toBits ^ ch.resize(32)
        if (ch == h"0A") line_count.din := line_count + 1
      bit_idx.din := bit_idx + 1
      cnt.din := cyclesPerBit - 1
    else busy.din := 0 // middle of the stop bit
  else cnt.din := cnt - 1
  end if
end uart_decoder
