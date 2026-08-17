// `lsu_dccm_ctl.sv`: the DCCM/PIC access path for the LSU pipe, DC1 -> DC3.
//
// It issues the DCCM read in DC1, captures the read data in DC3, merges store-buffer forwarding
// and PIC read data byte by byte, and right-justifies the result for the load return.
//
// Three notes on what the pinned build does to this module:
//
//   - **Every flop is on the root clock.** The six `rvdff_fpga` instances take the `rawclk` +
//     `clken` arm under RV_FPGA_OPTIMIZE and discard their `clk` input, and `rvdffe` degenerates
//     to `rvdffs`. So `lsu_freeze_c2_dc2_clk`, `lsu_freeze_c2_dc3_clk` and `lsu_dccm_c1_dc3_clk`
//     are dead inputs here: they are declared through their domain traits for port-list fidelity,
//     and the flops live in this design's own domain with the matching `*_clken` as the enable.
//     The four `lsu_dccm_c1_dc3_clken` flops share one enable, so they share one `if`.
//   - **Only the `DCCM_ENABLE == 1` arm is transcribed.** `RV_DCCM_ENABLE` is defined by all three
//     shipped configurations, so the `else` arm (which ties the six flop outputs to 0) is
//     unreachable in every build this port targets. It is pruned rather than written, because the
//     two arms differ in *declaration* -- `OUT.REG` against a driven `OUT` -- not just in value.
//   - **The three derived clock ports trail the port list**, where the baseline interleaves them
//     with the `*_clken` inputs. Names and widths are unchanged; only the order differs, and the
//     equivalence check pairs ports by name.
//
// Two transcription points where the baseline's spelling does not survive verbatim:
//
//   - `lsu_ld_data_dc3_nc` / `lsu_ld_data_corr_dc3_nc` are the baseline's way of absorbing the
//     upper 32 bits of a 64-bit shift into a named-but-unused wire, so the assignment can target
//     a concatenation. DFHDL takes the slice directly and the `_nc` wires do not exist.
//   - `picm_addr`'s two arms are `{17'b0, ...}` and `{{32-PIC_BITS{1'b0}}, ...}` in the baseline.
//     `PIC_BITS` is 15, so both are a zero-extension to 32 and both transcribe as `.resize(32)`.
//
// Upstream: chipsalliance/Cores-VeeR-EH1@915fb34, via RTLMeter designs/VeeR-EH1.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.veer_eh1

import dfhdl.*
import veer_types.*

class lsu_dccm_ctl
    extends RTDesign,
      globals, // `include "global.h"
      LsuFreezeC2Dc2Domain,
      LsuFreezeC2Dc3Domain,
      LsuDccmC1Dc3Domain:
  val lsu_freeze_c2_dc2_clken = Bit <> IN // clocks
  val lsu_freeze_c2_dc3_clken = Bit <> IN
  val lsu_dccm_c1_dc3_clken   = Bit <> IN
  val lsu_pic_c1_dc3_clken    = Bit <> IN

  val lsu_freeze_dc3 = Bit <> IN // freeze

  val lsu_pkt_dc3      = lsu_pkt_t          <> IN // lsu packets
  val lsu_pkt_dc1      = lsu_pkt_t          <> IN
  val addr_in_dccm_dc1 = Bit                <> IN // address maps to dccm
  val addr_in_pic_dc1  = Bit                <> IN // address maps to pic
  val addr_in_pic_dc3  = Bit                <> IN
  val lsu_addr_dc1     = Bits(32)           <> IN // starting byte address for loads
  val end_addr_dc1     = Bits(RV_DCCM_BITS) <> IN // last address used to calculate unaligned
  val lsu_addr_dc3     = Bits(RV_DCCM_BITS) <> IN // starting byte address for loads

  val stbuf_reqvld_any      = Bit                  <> IN // write enable
  val stbuf_addr_in_pic_any = Bit                  <> IN // stbuf is going to pic
  val stbuf_addr_any        = Bits(RV_LSU_SB_BITS) <> IN // stbuf address (aligned)

  val stbuf_data_any         = Bits(RV_DCCM_DATA_WIDTH) <> IN // the read out from stbuf
  val stbuf_ecc_any          = Bits(RV_DCCM_ECC_WIDTH)  <> IN // the encoded data with ECC bits
  val stbuf_fwddata_hi_dc3   = Bits(RV_DCCM_DATA_WIDTH) <> IN // stbuf fowarding to load
  val stbuf_fwddata_lo_dc3   = Bits(RV_DCCM_DATA_WIDTH) <> IN
  val stbuf_fwdbyteen_hi_dc3 = Bits(RV_DCCM_BYTE_WIDTH) <> IN
  val stbuf_fwdbyteen_lo_dc3 = Bits(RV_DCCM_BYTE_WIDTH) <> IN

  val lsu_double_ecc_error_dc3 = Bit                      <> IN // lsu has a DED
  val store_ecc_datafn_hi_dc3  = Bits(RV_DCCM_DATA_WIDTH) <> IN // store data
  val store_ecc_datafn_lo_dc3  = Bits(RV_DCCM_DATA_WIDTH) <> IN

  // the four DCCM capture flops, `rvdff_fpga` on `lsu_dccm_c1_dc3_clken`
  val dccm_data_hi_dc3     = Bits(RV_DCCM_DATA_WIDTH) <> OUT.REG init all(0) // data from the dccm
  val dccm_data_lo_dc3     = Bits(RV_DCCM_DATA_WIDTH) <> OUT.REG init all(0)
  val dccm_data_ecc_hi_dc3 = Bits(RV_DCCM_ECC_WIDTH)  <> OUT.REG init all(0) // data + ecc
  val dccm_data_ecc_lo_dc3 = Bits(RV_DCCM_ECC_WIDTH)  <> OUT.REG init all(0)
  // right justified, ie load byte will have data at 7:0
  val lsu_ld_data_dc3      = Bits(RV_DCCM_DATA_WIDTH) <> OUT
  val lsu_ld_data_corr_dc3 = Bits(RV_DCCM_DATA_WIDTH) <> OUT
  val picm_mask_data_dc3   = Bits(32)                 <> OUT // pic data to stbuf
  val lsu_stbuf_commit_any = Bit <> OUT // stbuf wins the dccm port or is to pic
  val lsu_dccm_rden_dc3    = Bit <> OUT.REG init 0 // dccm read

  val dccm_dma_rvalid    = Bit      <> OUT // dccm serviving the dma load
  val dccm_dma_ecc_error = Bit      <> OUT // DMA load had ecc error
  val dccm_dma_rdata     = Bits(64) <> OUT // dccm data to dma request

  // DCCM ports
  val dccm_wren       = Bit                <> OUT // dccm interface -- write
  val dccm_rden       = Bit                <> OUT // dccm interface -- read
  val dccm_wr_addr    = Bits(RV_DCCM_BITS) <> OUT // dccm interface -- wr addr
  val dccm_rd_addr_lo = Bits(RV_DCCM_BITS) <> OUT // dccm interface -- read address for lo bank
  val dccm_rd_addr_hi = Bits(RV_DCCM_BITS) <> OUT // dccm interface -- read address for hi bank
  val dccm_wr_data = Bits(RV_DCCM_FDATA_WIDTH) <> OUT

  val dccm_rd_data_lo = Bits(RV_DCCM_FDATA_WIDTH) <> IN // dccm read data back from the dccm
  val dccm_rd_data_hi = Bits(RV_DCCM_FDATA_WIDTH) <> IN

  // PIC ports
  val picm_wren    = Bit      <> OUT // write to pic
  val picm_rden    = Bit      <> OUT // read to pick
  val picm_mken    = Bit      <> OUT // write to pic need a mask
  val picm_addr    = Bits(32) <> OUT // address for pic access - shared between reads and write
  val picm_wr_data = Bits(32) <> OUT // write data
  val picm_rd_data = Bits(32) <> IN // read data

  val scan_mode = Bit <> IN // scan mode

  val DCCM_WIDTH_BITS: Int <> CONST = clog2(DCCM_BYTE_WIDTH)
  val PIC_BITS: Int <> CONST        = RV_PIC_BITS

  // `rvdffe`/`rvdff_fpga` outputs that the combinational logic below reads, so they are declared
  // here (as the baseline declares the wires) and driven in the flop section at the end.
  val lsu_dccm_rden_dc2   = Bit      <> VAR.REG init 0
  val picm_rd_data_lo_dc3 = Bits(32) <> VAR.REG init all(0)

  // `lsu_dccm_rden_dc1`, `picm_rden` and `picm_mken` are pulled ahead of `lsu_stbuf_commit_any`,
  // which reads all three; the baseline's `assign` order does not constrain it.

  // No need to read for aligned word/dword stores since ECC will come by new data completely
  val lsu_dccm_rden_dc1 = lsu_pkt_dc1.valid &
    (lsu_pkt_dc1.load |
      (lsu_pkt_dc1.store &
        (~(lsu_pkt_dc1.word | lsu_pkt_dc1.dword) | (lsu_addr_dc1(1, 0) != all(0))))) &
    addr_in_dccm_dc1

  picm_rden <> lsu_pkt_dc1.valid & lsu_pkt_dc1.load & addr_in_pic_dc1
  picm_mken <> lsu_pkt_dc1.valid & lsu_pkt_dc1.store & addr_in_pic_dc1 // Get the mask for stores

  lsu_stbuf_commit_any <> stbuf_reqvld_any & ~lsu_freeze_dc3 & (
    ~(lsu_dccm_rden_dc1 | picm_rden | picm_mken) |
      ((picm_rden | picm_mken) & ~stbuf_addr_in_pic_any) |
      (lsu_dccm_rden_dc1 &
        (stbuf_addr_in_pic_any | ~(
          (stbuf_addr_any(DCCM_WIDTH_BITS + DCCM_BANK_BITS - 1, DCCM_WIDTH_BITS) ==
            lsu_addr_dc1(DCCM_WIDTH_BITS + DCCM_BANK_BITS - 1, DCCM_WIDTH_BITS)) |
            (stbuf_addr_any(DCCM_WIDTH_BITS + DCCM_BANK_BITS - 1, DCCM_WIDTH_BITS) ==
              end_addr_dc1(DCCM_WIDTH_BITS + DCCM_BANK_BITS - 1, DCCM_WIDTH_BITS))
        )))
  )

  // DCCM inputs
  dccm_wren       <> lsu_stbuf_commit_any & ~stbuf_addr_in_pic_any
  dccm_rden       <> lsu_dccm_rden_dc1 & addr_in_dccm_dc1
  dccm_wr_addr    <> stbuf_addr_any(DCCM_BITS - 1, 0)
  dccm_rd_addr_lo <> lsu_addr_dc1(DCCM_BITS - 1, 0)
  dccm_rd_addr_hi <> end_addr_dc1(DCCM_BITS - 1, 0)
  dccm_wr_data    <> (stbuf_ecc_any, stbuf_data_any)

  // DCCM outputs
  val dccm_data_lo_dc2 = dccm_rd_data_lo(DCCM_DATA_WIDTH - 1, 0)
  val dccm_data_hi_dc2 = dccm_rd_data_hi(DCCM_DATA_WIDTH - 1, 0)

  val dccm_data_ecc_lo_dc2 = dccm_rd_data_lo(DCCM_FDATA_WIDTH - 1, DCCM_DATA_WIDTH)
  val dccm_data_ecc_hi_dc2 = dccm_rd_data_hi(DCCM_FDATA_WIDTH - 1, DCCM_DATA_WIDTH)

  // PIC signals. PIC ignores the lower 2 bits of address since PIC memory registers are 32-bits
  picm_wren <> lsu_stbuf_commit_any & stbuf_addr_in_pic_any
  picm_addr <> (picm_rden | picm_mken).sel(
    RV_PIC_BASE_ADDR | lsu_addr_dc1(14, 0).resize(32),
    RV_PIC_BASE_ADDR | stbuf_addr_any(RV_PIC_BITS - 1, 0).resize(32)
  )
  picm_wr_data <> stbuf_data_any(31, 0)

  picm_mask_data_dc3 <> picm_rd_data_lo_dc3
  val picm_rd_data_dc3: Bits[64] <> VAL = picm_rd_data_lo_dc3.repeat(2)

  val dccm_dout_dc3: Bits[64] <> VAL      = (dccm_data_hi_dc3, dccm_data_lo_dc3)
  val dccm_corr_dout_dc3: Bits[64] <> VAL = (store_ecc_datafn_hi_dc3, store_ecc_datafn_lo_dc3)
  val stbuf_fwddata_dc3: Bits[64] <> VAL  = (stbuf_fwddata_hi_dc3, stbuf_fwddata_lo_dc3)
  val stbuf_fwdbyteen_dc3: Bits[8] <> VAL = (stbuf_fwdbyteen_hi_dc3, stbuf_fwdbyteen_lo_dc3)

  // `for (genvar i=0; i<8; i++) begin: GenLoop`, a byte-wise three-way mux
  val lsu_rdata_dc3      = Bits(64) <> VAR
  val lsu_rdata_corr_dc3 = Bits(64) <> VAR
  for (i <- 0 until 8)
    lsu_rdata_dc3(8 * i + 7, 8 * i) <> stbuf_fwdbyteen_dc3(i).sel(
      stbuf_fwddata_dc3(8 * i + 7, 8 * i),
      addr_in_pic_dc3.sel(picm_rd_data_dc3(8 * i + 7, 8 * i), dccm_dout_dc3(8 * i + 7, 8 * i))
    )
    lsu_rdata_corr_dc3(8 * i + 7, 8 * i) <> stbuf_fwdbyteen_dc3(i).sel(
      stbuf_fwddata_dc3(8 * i + 7, 8 * i),
      addr_in_pic_dc3.sel(picm_rd_data_dc3(8 * i + 7, 8 * i), dccm_corr_dout_dc3(8 * i + 7, 8 * i))
    )

  dccm_dma_rvalid    <> lsu_pkt_dc3.valid & lsu_pkt_dc3.load & lsu_pkt_dc3.dma
  dccm_dma_ecc_error <> lsu_double_ecc_error_dc3
  // Need to replicate the data for non-dw access since ecc correction is done only in lower word
  dccm_dma_rdata <> lsu_pkt_dc3.dword.sel(lsu_rdata_corr_dc3, lsu_rdata_corr_dc3(31, 0).repeat(2))

  // `8*lsu_addr_dc3[1:0]`. The carry multiply is what keeps it exact: a plain `* 8` would take
  // the wider operand's 4 bits and lose the top of 3*8 == 24, and DFHDL wants a shift amount
  // exactly clog2(64) == 6 bits wide, which `*^` produces (2 + 4).
  val ld_shift = lsu_addr_dc3(1, 0).uint *^ 8
  lsu_ld_data_dc3      <> (lsu_rdata_dc3 >> ld_shift)(31, 0)
  lsu_ld_data_corr_dc3 <> (lsu_rdata_corr_dc3 >> ld_shift)(31, 0)

  // Flops
  if (lsu_pic_c1_dc3_clken) picm_rd_data_lo_dc3.din := picm_rd_data

  if (lsu_freeze_c2_dc2_clken) lsu_dccm_rden_dc2.din := lsu_dccm_rden_dc1
  if (lsu_freeze_c2_dc3_clken) lsu_dccm_rden_dc3.din := lsu_dccm_rden_dc2

  if (lsu_dccm_c1_dc3_clken)
    dccm_data_hi_dc3.din     := dccm_data_hi_dc2
    dccm_data_lo_dc3.din     := dccm_data_lo_dc2
    dccm_data_ecc_hi_dc3.din := dccm_data_ecc_hi_dc2
    dccm_data_ecc_lo_dc3.din := dccm_data_ecc_lo_dc2
end lsu_dccm_ctl
