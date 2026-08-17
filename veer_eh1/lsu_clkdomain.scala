// `lsu_clkdomain.sv`: the LSU's clock generation block.
//
// The one module in the port that *sources* derived clocks rather than consuming them, so every
// clock output is an `RTDerivedClkDomainSrc` whose `clk` identifies as `<domain>_clk` and
// auto-connects to every same-named consumer in scope. `free_clk` is the one derived clock it
// consumes (`veer.sv`'s `rvoclkhdr free_cg`), so that one is an `RTDerivedClkDomain`.
//
// What the pinned build reduces this module to, and it is worth stating plainly:
//
//   - `rvoclkhdr` is `assign l1clk = clk` under RV_FPGA_OPTIMIZE, so the 13 clocks it sources are
//     the root clock, ungated. The `en` term it is passed is dead on that path; the same
//     expression is also driven out as the matching `*_clken`, which is what consumers use.
//   - The `rvclkhdr` arm is not compiled, so the 8 clocks it would have sourced are tied to `1'b0`.
//     Every consumer of those is an `rvdff_fpga`/`rvdffs_fpga`, which under the same define takes
//     `rawclk` + `clken` and discards its `clk` input, so those 8 are genuinely dead wires.
//   - Every flop here therefore clocks on the root clock: five on `lsu_free_c2_clk` (= `clk`), one
//     on `free_clk` (= `clk`, gated by `.en(1'b1)`), four `rvdff_fpga` on `rawclk`. They are
//     declared in the design's own domain rather than in regions of the clocks the baseline names,
//     because in this build those clocks *are* this domain's clock.
//
// So the module's real content is the enable logic and 10 enabled flops. The clock outputs are kept
// as derived-clock sources regardless, because that is the structure the consumers are written
// against, and because a sourceless derived clock would otherwise surface at the top level.
//
// Upstream: chipsalliance/Cores-VeeR-EH1@915fb34, via RTLMeter designs/VeeR-EH1.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.veer_eh1

import dfhdl.*
import veer_types.*

class lsu_clkdomain extends RTDesign:
  val clk = Clk <> IN // read for the RV_FPGA_OPTIMIZE `assign l1clk = clk` pass-through

  val free = new RTDerivedClkDomain {} // `veer.sv`'s free-running clock, consumed here

  val clk_override             = Bit <> IN // chicken bit to turn off clock gating
  val lsu_freeze_dc3           = Bit <> IN // freeze
  val addr_in_dccm_dc2         = Bit <> IN // address in dccm
  val addr_in_pic_dc2          = Bit <> IN // address is in pic
  val dma_dccm_req             = Bit <> IN // dma is active
  val dma_mem_write            = Bit <> IN // dma write is active
  val load_stbuf_reqvld_dc3    = Bit <> IN // instruction to stbuf
  val store_stbuf_reqvld_dc3   = Bit <> IN // instruction to stbuf
  val stbuf_reqvld_any         = Bit <> IN // stbuf is draining
  val stbuf_reqvld_flushed_any = Bit <> IN // instruction going to stbuf is flushed
  val lsu_busreq_dc5           = Bit <> IN // busreq in dc5
  val lsu_bus_buffer_pend_any  = Bit <> IN // bus buffer has a pending bus entry
  val lsu_bus_buffer_empty_any = Bit <> IN // external bus buffer is empty
  val lsu_stbuf_empty_any      = Bit <> IN // stbuf is empty

  val lsu_bus_clk_en = Bit <> IN // bus clock enable

  val lsu_p       = lsu_pkt_t <> IN // lsu packet in decode
  val lsu_pkt_dc1 = lsu_pkt_t <> IN // lsu packet in dc1
  val lsu_pkt_dc2 = lsu_pkt_t <> IN // lsu packet in dc2
  val lsu_pkt_dc3 = lsu_pkt_t <> IN // lsu packet in dc3
  val lsu_pkt_dc4 = lsu_pkt_t <> IN // lsu packet in dc4
  val lsu_pkt_dc5 = lsu_pkt_t <> IN // lsu packet in dc5

  val lsu_store_c1_dc1_clken = Bit <> OUT // store in dc1
  val lsu_store_c1_dc2_clken = Bit <> OUT // store in dc2
  val lsu_store_c1_dc3_clken = Bit <> OUT // store in dc3

  val lsu_freeze_c1_dc1_clken = Bit <> OUT // freeze
  val lsu_freeze_c1_dc2_clken = Bit <> OUT
  val lsu_freeze_c1_dc3_clken = Bit <> OUT

  val lsu_freeze_c2_dc1_clken = Bit <> OUT
  val lsu_freeze_c2_dc2_clken = Bit <> OUT
  val lsu_freeze_c2_dc3_clken = Bit <> OUT
  val lsu_freeze_c2_dc4_clken = Bit <> OUT

  val lsu_dccm_c1_dc3_clken = Bit <> OUT
  val lsu_pic_c1_dc3_clken  = Bit <> OUT // pic clock enable

  val scan_mode = Bit <> IN

  // ---- clock sources ------------------------------------------------------------------------
  val lsu_c1_dc3 = new RTDerivedClkDomainSrc {} // dc3 pipe single pulse clock
  val lsu_c1_dc4 = new RTDerivedClkDomainSrc {} // dc4 pipe single pulse clock
  val lsu_c1_dc5 = new RTDerivedClkDomainSrc {} // dc5 pipe single pulse clock

  val lsu_c2_dc3 = new RTDerivedClkDomainSrc {} // dc3 pipe double pulse clock
  val lsu_c2_dc4 = new RTDerivedClkDomainSrc {} // dc4 pipe double pulse clock
  val lsu_c2_dc5 = new RTDerivedClkDomainSrc {} // dc5 pipe double pulse clock

  val lsu_store_c1_dc4 = new RTDerivedClkDomainSrc {} // store in dc4
  val lsu_store_c1_dc5 = new RTDerivedClkDomainSrc {} // store in dc5

  val lsu_stbuf_c1    = new RTDerivedClkDomainSrc {}
  val lsu_bus_obuf_c1 = new RTDerivedClkDomainSrc {} // obuf clock
  val lsu_bus_ibuf_c1 = new RTDerivedClkDomainSrc {} // ibuf clock
  val lsu_bus_buf_c1  = new RTDerivedClkDomainSrc {} // buf clock

  val lsu_free_c2 = new RTDerivedClkDomainSrc {}

  // the `ifdef RV_FPGA_OPTIMIZE arm ties these off; consumers take `rawclk` and ignore them
  val lsu_freeze_c1_dc2 = new RTDerivedClkDomainSrc {}
  val lsu_freeze_c1_dc3 = new RTDerivedClkDomainSrc {}
  val lsu_freeze_c2_dc1 = new RTDerivedClkDomainSrc {}
  val lsu_freeze_c2_dc2 = new RTDerivedClkDomainSrc {}
  val lsu_freeze_c2_dc3 = new RTDerivedClkDomainSrc {}
  val lsu_freeze_c2_dc4 = new RTDerivedClkDomainSrc {}
  val lsu_busm          = new RTDerivedClkDomainSrc {} // bus clock
  val lsu_dccm_c1_dc3   = new RTDerivedClkDomainSrc {} // dccm clock

  // ---- flops --------------------------------------------------------------------------------
  // `.din` follows the enable logic below, since the enables read these back.
  val lsu_free_c1_clken_q = Bit <> VAR.REG init 0

  val lsu_c1_dc1_clken_q = Bit <> VAR.REG init 0
  val lsu_c1_dc2_clken_q = Bit <> VAR.REG init 0
  val lsu_c1_dc3_clken_q = Bit <> VAR.REG init 0
  val lsu_c1_dc4_clken_q = Bit <> VAR.REG init 0
  val lsu_c1_dc5_clken_q = Bit <> VAR.REG init 0

  val lsu_freeze_c1_dc1_clken_q = Bit <> VAR.REG init 0
  val lsu_freeze_c1_dc2_clken_q = Bit <> VAR.REG init 0
  val lsu_freeze_c1_dc3_clken_q = Bit <> VAR.REG init 0
  val lsu_freeze_c1_dc4_clken_q = Bit <> VAR.REG init 0

  // ---- Clock Enable logic -------------------------------------------------------------------

  // Also use the flopped clock enable. We want to turn on the clocks from dc1->dc5 even if there
  // is a freeze
  val lsu_c1_dc1_clken = lsu_p.valid | dma_dccm_req | clk_override
  val lsu_c1_dc2_clken = lsu_pkt_dc1.valid | lsu_c1_dc1_clken_q | clk_override
  val lsu_c1_dc3_clken = lsu_pkt_dc2.valid | lsu_c1_dc2_clken_q | clk_override
  val lsu_c1_dc4_clken = lsu_pkt_dc3.valid | lsu_c1_dc3_clken_q | clk_override
  val lsu_c1_dc5_clken = lsu_pkt_dc4.valid | lsu_c1_dc4_clken_q | clk_override

  val lsu_c2_dc3_clken = lsu_c1_dc3_clken | lsu_c1_dc3_clken_q | clk_override
  val lsu_c2_dc4_clken = lsu_c1_dc4_clken | lsu_c1_dc4_clken_q | clk_override
  val lsu_c2_dc5_clken = lsu_c1_dc5_clken | lsu_c1_dc5_clken_q | clk_override

  lsu_store_c1_dc1_clken <> ((lsu_c1_dc1_clken & (lsu_p.store | dma_mem_write)) | clk_override) &
    ~lsu_freeze_dc3
  lsu_store_c1_dc2_clken <> ((lsu_c1_dc2_clken & lsu_pkt_dc1.store) | clk_override) &
    ~lsu_freeze_dc3
  lsu_store_c1_dc3_clken <> ((lsu_c1_dc3_clken & lsu_pkt_dc2.store) | clk_override) &
    ~lsu_freeze_dc3
  val lsu_store_c1_dc4_clken = (lsu_c1_dc4_clken & lsu_pkt_dc3.store) | clk_override
  val lsu_store_c1_dc5_clken = (lsu_c1_dc5_clken & lsu_pkt_dc4.store) | clk_override

  lsu_freeze_c1_dc1_clken <> (lsu_p.valid | dma_dccm_req | clk_override) & ~lsu_freeze_dc3
  lsu_freeze_c1_dc2_clken <> (lsu_pkt_dc1.valid | clk_override) & ~lsu_freeze_dc3
  lsu_freeze_c1_dc3_clken <> (lsu_pkt_dc2.valid | clk_override) & ~lsu_freeze_dc3
  val lsu_freeze_c1_dc4_clken = (lsu_pkt_dc3.valid | clk_override) & ~lsu_freeze_dc3

  lsu_freeze_c2_dc1_clken <> (lsu_freeze_c1_dc1_clken | lsu_freeze_c1_dc1_clken_q | clk_override) &
    ~lsu_freeze_dc3
  lsu_freeze_c2_dc2_clken <> (lsu_freeze_c1_dc2_clken | lsu_freeze_c1_dc2_clken_q | clk_override) &
    ~lsu_freeze_dc3
  lsu_freeze_c2_dc3_clken <> (lsu_freeze_c1_dc3_clken | lsu_freeze_c1_dc3_clken_q | clk_override) &
    ~lsu_freeze_dc3
  lsu_freeze_c2_dc4_clken <> (lsu_freeze_c1_dc4_clken | lsu_freeze_c1_dc4_clken_q | clk_override) &
    ~lsu_freeze_dc3

  val lsu_stbuf_c1_clken = load_stbuf_reqvld_dc3 | store_stbuf_reqvld_dc3 | stbuf_reqvld_any |
    stbuf_reqvld_flushed_any | clk_override
  val lsu_bus_ibuf_c1_clken = lsu_busreq_dc5 | clk_override
  val lsu_bus_obuf_c1_clken =
    ((lsu_bus_buffer_pend_any | lsu_busreq_dc5) & lsu_bus_clk_en) | clk_override
  val lsu_bus_buf_c1_clken = ~lsu_bus_buffer_empty_any | lsu_busreq_dc5 | clk_override

  lsu_dccm_c1_dc3_clken <> ((lsu_c1_dc3_clken & addr_in_dccm_dc2) | clk_override) & ~lsu_freeze_dc3
  lsu_pic_c1_dc3_clken  <> ((lsu_c1_dc3_clken & addr_in_pic_dc2) | clk_override) & ~lsu_freeze_dc3

  val lsu_free_c1_clken =
    (lsu_p.valid | lsu_pkt_dc1.valid | lsu_pkt_dc2.valid | lsu_pkt_dc3.valid | lsu_pkt_dc4.valid |
      lsu_pkt_dc5.valid) | ~lsu_bus_buffer_empty_any | ~lsu_stbuf_empty_any | clk_override
  val lsu_free_c2_clken = lsu_free_c1_clken | lsu_free_c1_clken_q | clk_override

  // ---- flop inputs --------------------------------------------------------------------------
  lsu_free_c1_clken_q.din := lsu_free_c1_clken

  lsu_c1_dc1_clken_q.din := lsu_c1_dc1_clken
  lsu_c1_dc2_clken_q.din := lsu_c1_dc2_clken
  lsu_c1_dc3_clken_q.din := lsu_c1_dc3_clken
  lsu_c1_dc4_clken_q.din := lsu_c1_dc4_clken
  lsu_c1_dc5_clken_q.din := lsu_c1_dc5_clken

  // `rvdff_fpga` takes `rawclk` + `clken`, so these are plain enabled registers
  if (lsu_freeze_c2_dc1_clken) lsu_freeze_c1_dc1_clken_q.din := lsu_freeze_c1_dc1_clken
  if (lsu_freeze_c2_dc2_clken) lsu_freeze_c1_dc2_clken_q.din := lsu_freeze_c1_dc2_clken
  if (lsu_freeze_c2_dc3_clken) lsu_freeze_c1_dc3_clken_q.din := lsu_freeze_c1_dc3_clken
  if (lsu_freeze_c2_dc4_clken) lsu_freeze_c1_dc4_clken_q.din := lsu_freeze_c1_dc4_clken

  // ---- Clock Headers ------------------------------------------------------------------------
  lsu_c1_dc3.clk <> clk.actual.as(lsu_c1_dc3.Clk)
  lsu_c1_dc4.clk <> clk.actual.as(lsu_c1_dc4.Clk)
  lsu_c1_dc5.clk <> clk.actual.as(lsu_c1_dc5.Clk)

  lsu_c2_dc3.clk <> clk.actual.as(lsu_c2_dc3.Clk)
  lsu_c2_dc4.clk <> clk.actual.as(lsu_c2_dc4.Clk)
  lsu_c2_dc5.clk <> clk.actual.as(lsu_c2_dc5.Clk)

  lsu_store_c1_dc4.clk <> clk.actual.as(lsu_store_c1_dc4.Clk)
  lsu_store_c1_dc5.clk <> clk.actual.as(lsu_store_c1_dc5.Clk)

  if (RV_FPGA_OPTIMIZE)
    lsu_freeze_c1_dc2.clk <> 0.as(lsu_freeze_c1_dc2.Clk)
    lsu_freeze_c1_dc3.clk <> 0.as(lsu_freeze_c1_dc3.Clk)
    lsu_freeze_c2_dc1.clk <> 0.as(lsu_freeze_c2_dc1.Clk)
    lsu_freeze_c2_dc2.clk <> 0.as(lsu_freeze_c2_dc2.Clk)
    lsu_freeze_c2_dc3.clk <> 0.as(lsu_freeze_c2_dc3.Clk)
    lsu_freeze_c2_dc4.clk <> 0.as(lsu_freeze_c2_dc4.Clk)
    lsu_busm.clk          <> 0.as(lsu_busm.Clk)
    lsu_dccm_c1_dc3.clk   <> 0.as(lsu_dccm_c1_dc3.Clk)
  else
    // `rvclkhdr`, the latch-based ICG, is not ported (plan decision 4) and is not compiled in the
    // pinned build. This arm keeps the `ifdef` structure and is unreachable here.
    lsu_freeze_c1_dc2.clk <> (clk.actual & lsu_freeze_c1_dc2_clken).as(lsu_freeze_c1_dc2.Clk)
    lsu_freeze_c1_dc3.clk <> (clk.actual & lsu_freeze_c1_dc3_clken).as(lsu_freeze_c1_dc3.Clk)
    lsu_freeze_c2_dc1.clk <> (clk.actual & lsu_freeze_c2_dc1_clken).as(lsu_freeze_c2_dc1.Clk)
    lsu_freeze_c2_dc2.clk <> (clk.actual & lsu_freeze_c2_dc2_clken).as(lsu_freeze_c2_dc2.Clk)
    lsu_freeze_c2_dc3.clk <> (clk.actual & lsu_freeze_c2_dc3_clken).as(lsu_freeze_c2_dc3.Clk)
    lsu_freeze_c2_dc4.clk <> (clk.actual & lsu_freeze_c2_dc4_clken).as(lsu_freeze_c2_dc4.Clk)
    lsu_busm.clk          <> (clk.actual & lsu_bus_clk_en).as(lsu_busm.Clk)
    lsu_dccm_c1_dc3.clk   <> (clk.actual & lsu_dccm_c1_dc3_clken).as(lsu_dccm_c1_dc3.Clk)
  end if

  lsu_stbuf_c1.clk    <> clk.actual.as(lsu_stbuf_c1.Clk)
  lsu_bus_ibuf_c1.clk <> clk.actual.as(lsu_bus_ibuf_c1.Clk)
  lsu_bus_obuf_c1.clk <> clk.actual.as(lsu_bus_obuf_c1.Clk)
  lsu_bus_buf_c1.clk  <> clk.actual.as(lsu_bus_buf_c1.Clk)

  lsu_free_c2.clk <> clk.actual.as(lsu_free_c2.Clk)
end lsu_clkdomain
