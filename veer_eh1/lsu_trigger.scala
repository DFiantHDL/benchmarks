// `lsu_trigger.sv`: the LSU's debug-trigger match logic.
//
// Purely combinational despite its ports: it takes `clk`, `lsu_free_c2_clk` and `rst_l` and uses
// none of them, because the four `rvmaskandmatch` instances are combinational and there is no flop
// in the module. The clocks are declared anyway (`FreeC2Domain` for the derived one) so the port
// list matches the baseline and `lsu`'s instantiation wires the same way.
//
// The four trigger channels are a `generate for`, so they transcribe as a Scala `for` over four
// `rvmaskandmatch` instances.
//
// Upstream: chipsalliance/Cores-VeeR-EH1@915fb34, via RTLMeter designs/VeeR-EH1.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.veer_eh1

import dfhdl.*
import veer_types.*

class lsu_trigger extends RTDesign, FreeC2Domain:
  // declared only to match the baseline's port list: with no flop here, nothing resets
  val rst_l = Rst <> IN

  val trigger_pkt_any = trigger_pkt_t X 4 <> IN // trigger packet from dec
  val lsu_pkt_dc3     = lsu_pkt_t         <> IN // lsu packet
  val lsu_addr_dc3    = Bits(32)          <> IN // address
  val lsu_result_dc3  = Bits(32)          <> IN // load data
  val store_data_dc3  = Bits(32)          <> IN // store data

  val lsu_trigger_match_dc3 = Bits(4) <> OUT // match result

  val store_data_trigger_dc3: Bits[32] <> VAL = (
    lsu_pkt_dc3.word.repeat(16) & store_data_dc3(31, 16),
    (lsu_pkt_dc3.half | lsu_pkt_dc3.word).repeat(8) & store_data_dc3(15, 8),
    store_data_dc3(7, 0)
  )

  val lsu_trigger_data_match = Bits(4) <> VAR

  for (i <- 0 until 4)
    val lsu_match_data = (~trigger_pkt_any(i).select).repeat(32) & lsu_addr_dc3 |
      (trigger_pkt_any(i).select & trigger_pkt_any(i).store).repeat(32) & store_data_trigger_dc3

    val trigger_match = rvmaskandmatch()
    trigger_match.mask        <> trigger_pkt_any(i).tdata2
    trigger_match.data        <> lsu_match_data
    trigger_match.masken      <> trigger_pkt_any(i).`match`
    lsu_trigger_data_match(i) <> trigger_match.`match`

    lsu_trigger_match_dc3(i) <> lsu_pkt_dc3.valid & ~lsu_pkt_dc3.dma &
      ((trigger_pkt_any(i).store & lsu_pkt_dc3.store) |
        (trigger_pkt_any(i).load & lsu_pkt_dc3.load & ~trigger_pkt_any(i).select)) &
      lsu_trigger_data_match(i)
  end for
end lsu_trigger
