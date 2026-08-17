// `global.h`, VeeR-EH1's per-module localparam header.
//
// Body-scoped, not global: it is included inside 20 module bodies (lsu_dccm_mem.sv:51,
// dbg.sv:118, dec_ib_ctl.sv:109, ...), so each of those designs carries these as members. A trait
// the design mixes in reproduces that exactly, and the emitted SystemVerilog shows it:
//
//   module lsu_dccm_ctl( ... );
//     `include "dfhdl_defs.svh"                        // <- common_defines.vh, the global include
//     localparam int DCCM_BITS = RV_DCCM_BITS;         // <- global.h, the body-scoped include
//
// The port plan originally specified `object globals` + `export globals.*`. That also makes the
// names real members, but it emits them into the *shared* `<Top>_defs.svh` alongside the `RV_*`
// macros, collapsing the two-tier structure the baseline has. It also trips a DFHDL elaboration
// crash (DFHDL#494): the first use of an object-scoped `Int <> CONST` that aliases another const,
// when that use is the left operand of `-`, fails with `Missing ref "TW_..."`. The trait form is
// the better transcription independently of the bug.
//
// Upstream: chipsalliance/Cores-VeeR-EH1@915fb34, via RTLMeter designs/VeeR-EH1.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.veer_eh1

import dfhdl.*

trait globals extends RTDesign:
  val TOTAL_INT: Int <> CONST = RV_PIC_TOTAL_INT_PLUS1

  val DCCM_BITS: Int <> CONST        = RV_DCCM_BITS
  val DCCM_BANK_BITS: Int <> CONST   = RV_DCCM_BANK_BITS
  val DCCM_NUM_BANKS: Int <> CONST   = RV_DCCM_NUM_BANKS
  val DCCM_DATA_WIDTH: Int <> CONST  = RV_DCCM_DATA_WIDTH
  val DCCM_FDATA_WIDTH: Int <> CONST = RV_DCCM_FDATA_WIDTH
  val DCCM_BYTE_WIDTH: Int <> CONST  = RV_DCCM_BYTE_WIDTH
  val DCCM_ECC_WIDTH: Int <> CONST   = RV_DCCM_ECC_WIDTH

  val LSU_RDBUF_DEPTH: Int <> CONST = RV_LSU_NUM_NBLOAD
  val DMA_BUF_DEPTH: Int <> CONST   = RV_DMA_BUF_DEPTH
  val LSU_STBUF_DEPTH: Int <> CONST = RV_LSU_STBUF_DEPTH
  val LSU_SB_BITS: Int <> CONST     = RV_LSU_SB_BITS

  val DEC_INSTBUF_DEPTH: Int <> CONST = RV_DEC_INSTBUF_DEPTH

  val ICCM_SIZE: Int <> CONST       = RV_ICCM_SIZE
  val ICCM_BITS: Int <> CONST       = RV_ICCM_BITS
  val ICCM_NUM_BANKS: Int <> CONST  = RV_ICCM_NUM_BANKS
  val ICCM_BANK_BITS: Int <> CONST  = RV_ICCM_BANK_BITS
  val ICCM_INDEX_BITS: Int <> CONST = RV_ICCM_INDEX_BITS
  // integer division, as in the baseline: 4 + (3 / 4) == 4
  val ICCM_BANK_HI: Int <> CONST = 4 + (RV_ICCM_BANK_BITS / 4)

  val ICACHE_TAG_HIGH: Int <> CONST  = RV_ICACHE_TAG_HIGH
  val ICACHE_TAG_LOW: Int <> CONST   = RV_ICACHE_TAG_LOW
  val ICACHE_IC_DEPTH: Int <> CONST  = RV_ICACHE_IC_DEPTH
  val ICACHE_TAG_DEPTH: Int <> CONST = RV_ICACHE_TAG_DEPTH

  val LSU_BUS_TAG: Int <> CONST = RV_LSU_BUS_TAG
  val DMA_BUS_TAG: Int <> CONST = RV_DMA_BUS_TAG
  val SB_BUS_TAG: Int <> CONST  = RV_SB_BUS_TAG
  val IFU_BUS_TAG: Int <> CONST = RV_IFU_BUS_TAG
end globals
