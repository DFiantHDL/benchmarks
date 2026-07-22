// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.protocol_engine
import dfhdl.*

enum RxPhase extends Encoded:
  case Idle, Sync, Data, Check, Gap

/** A long-running packet protocol engine, the DFacsimile-vs-Verilator benchmark FSM. A design-body
  * (concurrent) 16-bit Fibonacci LFSR generates the stimulus stream; the main process is a
  * high-level FSM over it:
  *
  *   - `Hunt`: a self-looping step that samples the LFSR once per cycle until its low nibble
  *     matches the `syncPattern` parameter
  *   - `Capture`: latches the pseudo-random payload length (0..15 bytes) and clears the running
  *     checksum
  *   - payload: a dynamic-bound `while` loop consuming one byte per cycle, folding each byte into
  *     an xorshift32 signature, an 8-bit checksum, and a 128-bit byte-trail shift register
  *   - `Verdict`: a parity check accepts or drops the packet, then a match dispatch either re-hunts
  *     immediately or falls through to a data-dependent gap wait of 1 to 4 cycles
  *
  * A second, independent process toggles a slow heartbeat, and the engine phase is exported for
  * observability. The design is closed (no inputs), so a block-less simulation run is fully
  * deterministic and the same cycle count must yield identical state in any simulator.
  */
class ProtocolEngine(
    val syncPattern: Bits[4] <> CONST = b"1011"
) extends RTDesign:
  /** running xorshift32 signature over all payload bytes */
  val sig = Bits(32) <> OUT.REG init all(0)

  /** the last 16 payload bytes, newest byte at the low end */
  val trail = Bits(128) <> OUT.REG init all(0)

  /** accepted packet count */
  val packets = UInt(32) <> OUT.REG init 0

  /** dropped packet count (checksum parity failed) */
  val drops = UInt(32) <> OUT.REG init 0

  /** current engine phase */
  val phase = RxPhase <> OUT.REG init RxPhase.Idle

  /** heartbeat from the independent slow process */
  val beat = Bit <> OUT.REG init 0

  // free-running Fibonacci LFSR (x^16 + x^14 + x^13 + x^11 + 1), the data source
  val lfsr = Bits(16) <> VAR.REG init h"ace1"
  lfsr.din := (lfsr(0) ^ lfsr(2) ^ lfsr(3) ^ lfsr(5)).bits ++ lfsr(15, 1)

  val len = UInt(4) <> VAR.REG init 0
  val idx = UInt(4) <> VAR.REG init 0
  val csum = Bits(8) <> VAR.REG init all(0)

  // data-dependent inter-packet gap length (1..4 cycles), sampled by the dynamic wait
  val gap = UInt(3) <> VAR
  gap := csum(1, 0).uint +^ 1

  process:
    def Hunt: Step =
      phase.din := RxPhase.Idle
      if (lfsr(3, 0) == syncPattern) NextStep else ThisStep
    def Capture: Step =
      phase.din := RxPhase.Sync
      len.din := lfsr(7, 4).uint
      idx.din := 0
      csum.din := all(0)
      NextStep
    while (idx < len)
      val d = lfsr(7, 0)
      phase.din := RxPhase.Data
      csum.din := csum ^ d
      val x1 = sig ^ (sig << 13)
      val x2 = x1 ^ (x1 >> 17)
      val x3 = x2 ^ (x2 << 5)
      sig.din := x3 ^ d.resize(32)
      trail.din := trail(119, 0) ++ d
      idx.din := idx + 1
      1.cy.wait
    def Verdict: Step =
      phase.din := RxPhase.Check
      if (csum(0) ^ csum(7)) drops.din := drops + 1
      else packets.din := packets + 1
      lfsr(1, 0) match
        case b"11" => Hunt
        case _     => NextStep
    phase.din := RxPhase.Gap
    gap.cy.wait

  process:
    1024.cy.wait
    beat.din := !beat
end ProtocolEngine
