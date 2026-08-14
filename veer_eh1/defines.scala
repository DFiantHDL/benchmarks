// The pinned `default` configuration of RTLMeter's VeeR-EH1, as `common_defines.vh` sets it.
//
// A global include, so these are top-level definitions in the package. `global.h` is body-scoped
// and lives in globals.scala instead.
//
// Upstream: chipsalliance/Cores-VeeR-EH1@915fb34, via RTLMeter designs/VeeR-EH1.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.veer_eh1

import dfhdl.*

// ---------------------------------------------------------------------------------------------
// Elaboration switches: only ever `ifdef-tested, never read as values.
// ---------------------------------------------------------------------------------------------

/** Defined by the pinned config. The build therefore has NO clock gating: `rvoclkhdr` is a wire
  * (`assign l1clk = clk`), `rvclkhdr` is not even compiled, and `rvdffe` degenerates to `rvdffs`.
  * Every `*_clk` a module receives is the root clock. See the port plan.
  */
inline val RV_FPGA_OPTIMIZE = true

/** `define ASSERT_ON at the top of common_defines.vh is retracted by `undef on its last line. */
inline val ASSERT_ON = false

// ---------------------------------------------------------------------------------------------
// SRAM cell selection. Plain Scala Strings: they name a module in the baseline and pick a
// (depth, width) here, so they are elaboration-only and never enter the IR.
// ---------------------------------------------------------------------------------------------

inline val RV_DCCM_DATA_CELL   = "ram_2048x39" // 2048 x 39
inline val RV_ICACHE_DATA_CELL = "ram_256x34" // 256 x 34
inline val RV_ICACHE_TAG_CELL  = "ram_64x21" // 64 x 21
inline val RV_ICCM_DATA_CELL   = "ram_16384x39" // 16384 x 39

// ---------------------------------------------------------------------------------------------
// Numeric configuration constants.
// ---------------------------------------------------------------------------------------------

val RV_BHT_ADDR_HI: Int <> CONST          = 7
val RV_BHT_ADDR_LO: Int <> CONST          = 4
val RV_BHT_ARRAY_DEPTH: Int <> CONST      = 16
val RV_BHT_GHR_SIZE: Int <> CONST         = 5
val RV_BTB_ADDR_HI: Int <> CONST          = 5
val RV_BTB_ADDR_LO: Int <> CONST          = 4
val RV_BTB_ARRAY_DEPTH: Int <> CONST      = 4
val RV_BTB_BTAG_FOLD: Int <> CONST        = 1
val RV_BTB_BTAG_SIZE: Int <> CONST        = 9
val RV_BTB_INDEX1_HI: Int <> CONST        = 5
val RV_BTB_INDEX1_LO: Int <> CONST        = 4
val RV_BTB_INDEX2_HI: Int <> CONST        = 7
val RV_BTB_INDEX2_LO: Int <> CONST        = 6
val RV_BTB_INDEX3_HI: Int <> CONST        = 9
val RV_BTB_INDEX3_LO: Int <> CONST        = 8
val RV_BUILD_AXI4: Int <> CONST           = 1
val RV_DCCM_BANK_BITS: Int <> CONST       = 3
val RV_DCCM_BITS: Int <> CONST            = 16
val RV_DCCM_BYTE_WIDTH: Int <> CONST      = 4
val RV_DCCM_DATA_WIDTH: Int <> CONST      = 32
val RV_DCCM_ECC_WIDTH: Int <> CONST       = 7
val RV_DCCM_ENABLE: Int <> CONST          = 1
val RV_DCCM_FDATA_WIDTH: Int <> CONST     = 39
val RV_DCCM_NUM_BANKS: Int <> CONST       = 8
val RV_DCCM_SIZE: Int <> CONST            = 64
val RV_DEC_INSTBUF_DEPTH: Int <> CONST    = 4
val RV_DMA_BUF_DEPTH: Int <> CONST        = 4
val RV_DMA_BUS_TAG: Int <> CONST          = 1
val RV_ICACHE_ENABLE: Int <> CONST        = 1
val RV_ICACHE_IC_DEPTH: Int <> CONST      = 8
val RV_ICACHE_TAG_DEPTH: Int <> CONST     = 64
val RV_ICACHE_TAG_HIGH: Int <> CONST      = 12
val RV_ICACHE_TAG_LOW: Int <> CONST       = 6
val RV_ICCM_BANK_BITS: Int <> CONST       = 3
val RV_ICCM_BITS: Int <> CONST            = 19
val RV_ICCM_ENABLE: Int <> CONST          = 1
val RV_ICCM_INDEX_BITS: Int <> CONST      = 14
val RV_ICCM_NUM_BANKS: Int <> CONST       = 8
val RV_ICCM_SIZE: Int <> CONST            = 512
val RV_IFU_BUS_TAG: Int <> CONST          = 3
val RV_LSU_BUS_TAG: Int <> CONST          = 4
val RV_LSU_NUM_NBLOAD: Int <> CONST       = 8
val RV_LSU_NUM_NBLOAD_WIDTH: Int <> CONST = 3
val RV_LSU_SB_BITS: Int <> CONST          = 16
val RV_LSU_STBUF_DEPTH: Int <> CONST      = 8
val RV_PIC_BITS: Int <> CONST             = 15
val RV_PIC_SIZE: Int <> CONST             = 32
val RV_PIC_TOTAL_INT: Int <> CONST        = 8
val RV_PIC_TOTAL_INT_PLUS1: Int <> CONST  = 9
val RV_RET_STACK_SIZE: Int <> CONST       = 4
val RV_SB_BUS_TAG: Int <> CONST           = 1

// ---------------------------------------------------------------------------------------------
// Address, region and mask constants. Sized Verilog literals, kept at their baseline widths
// (an unsized `'h...` literal is 32-bit in Verilog, so that is the width used here).
// ---------------------------------------------------------------------------------------------

val RV_DATA_ACCESS_ADDR0: Bits[32] <> CONST  = h"32'00000000"
val RV_DATA_ACCESS_ADDR1: Bits[32] <> CONST  = h"32'00000000"
val RV_DATA_ACCESS_ADDR2: Bits[32] <> CONST  = h"32'00000000"
val RV_DATA_ACCESS_ADDR3: Bits[32] <> CONST  = h"32'00000000"
val RV_DATA_ACCESS_ADDR4: Bits[32] <> CONST  = h"32'00000000"
val RV_DATA_ACCESS_ADDR5: Bits[32] <> CONST  = h"32'00000000"
val RV_DATA_ACCESS_ADDR6: Bits[32] <> CONST  = h"32'00000000"
val RV_DATA_ACCESS_ADDR7: Bits[32] <> CONST  = h"32'00000000"
val RV_DATA_ACCESS_ENABLE0: Bits[1] <> CONST = h"1'0"
val RV_DATA_ACCESS_ENABLE1: Bits[1] <> CONST = h"1'0"
val RV_DATA_ACCESS_ENABLE2: Bits[1] <> CONST = h"1'0"
val RV_DATA_ACCESS_ENABLE3: Bits[1] <> CONST = h"1'0"
val RV_DATA_ACCESS_ENABLE4: Bits[1] <> CONST = h"1'0"
val RV_DATA_ACCESS_ENABLE5: Bits[1] <> CONST = h"1'0"
val RV_DATA_ACCESS_ENABLE6: Bits[1] <> CONST = h"1'0"
val RV_DATA_ACCESS_ENABLE7: Bits[1] <> CONST = h"1'0"
val RV_DATA_ACCESS_MASK0: Bits[32] <> CONST  = h"32'ffffffff"
val RV_DATA_ACCESS_MASK1: Bits[32] <> CONST  = h"32'ffffffff"
val RV_DATA_ACCESS_MASK2: Bits[32] <> CONST  = h"32'ffffffff"
val RV_DATA_ACCESS_MASK3: Bits[32] <> CONST  = h"32'ffffffff"
val RV_DATA_ACCESS_MASK4: Bits[32] <> CONST  = h"32'ffffffff"
val RV_DATA_ACCESS_MASK5: Bits[32] <> CONST  = h"32'ffffffff"
val RV_DATA_ACCESS_MASK6: Bits[32] <> CONST  = h"32'ffffffff"
val RV_DATA_ACCESS_MASK7: Bits[32] <> CONST  = h"32'ffffffff"
val RV_DCCM_REGION: Bits[4] <> CONST         = h"4'f"
val RV_DCCM_SADR: Bits[32] <> CONST          = h"32'f0040000"
val RV_ICCM_REGION: Bits[4] <> CONST         = h"4'e"
val RV_ICCM_SADR: Bits[32] <> CONST          = h"32'ee000000"
val RV_INST_ACCESS_ADDR0: Bits[32] <> CONST  = h"32'00000000"
val RV_INST_ACCESS_ADDR1: Bits[32] <> CONST  = h"32'00000000"
val RV_INST_ACCESS_ADDR2: Bits[32] <> CONST  = h"32'00000000"
val RV_INST_ACCESS_ADDR3: Bits[32] <> CONST  = h"32'00000000"
val RV_INST_ACCESS_ADDR4: Bits[32] <> CONST  = h"32'00000000"
val RV_INST_ACCESS_ADDR5: Bits[32] <> CONST  = h"32'00000000"
val RV_INST_ACCESS_ADDR6: Bits[32] <> CONST  = h"32'00000000"
val RV_INST_ACCESS_ADDR7: Bits[32] <> CONST  = h"32'00000000"
val RV_INST_ACCESS_ENABLE0: Bits[1] <> CONST = h"1'0"
val RV_INST_ACCESS_ENABLE1: Bits[1] <> CONST = h"1'0"
val RV_INST_ACCESS_ENABLE2: Bits[1] <> CONST = h"1'0"
val RV_INST_ACCESS_ENABLE3: Bits[1] <> CONST = h"1'0"
val RV_INST_ACCESS_ENABLE4: Bits[1] <> CONST = h"1'0"
val RV_INST_ACCESS_ENABLE5: Bits[1] <> CONST = h"1'0"
val RV_INST_ACCESS_ENABLE6: Bits[1] <> CONST = h"1'0"
val RV_INST_ACCESS_ENABLE7: Bits[1] <> CONST = h"1'0"
val RV_INST_ACCESS_MASK0: Bits[32] <> CONST  = h"32'ffffffff"
val RV_INST_ACCESS_MASK1: Bits[32] <> CONST  = h"32'ffffffff"
val RV_INST_ACCESS_MASK2: Bits[32] <> CONST  = h"32'ffffffff"
val RV_INST_ACCESS_MASK3: Bits[32] <> CONST  = h"32'ffffffff"
val RV_INST_ACCESS_MASK4: Bits[32] <> CONST  = h"32'ffffffff"
val RV_INST_ACCESS_MASK5: Bits[32] <> CONST  = h"32'ffffffff"
val RV_INST_ACCESS_MASK6: Bits[32] <> CONST  = h"32'ffffffff"
val RV_INST_ACCESS_MASK7: Bits[32] <> CONST  = h"32'ffffffff"
val RV_PIC_BASE_ADDR: Bits[32] <> CONST      = h"32'f00c0000"
val RV_PIC_REGION: Bits[4] <> CONST          = h"4'f"

// ---------------------------------------------------------------------------------------------
// Deliberately not defined here
//
// Verilog code-fragment macros -- these expand to expressions, not values, so they become
// methods on the design that uses them (all four are ifu_bp_ctl's):
//   RV_BHT_GHR_PAD, RV_BHT_GHR_PAD2, RV_BHT_GHR_RANGE, RV_BHT_HASH_STRING
//
// Testbench and toolchain macros -- out of the ported scope:
//   TOP, RV_TOP, CPU_TOP, RV_TARGET, TEC_RV_ICG, CLOCK_PERIOD, SDVT_AHB
//
// Defined by the config but never referenced by the ported design (69):
//   CLOCK_PERIOD, DATAWIDTH, REGWIDTH, RV_BHT_SIZE
//   RV_BTB_SIZE, RV_DCCM_EADR, RV_DCCM_INDEX_BITS, RV_DCCM_NUM_BANKS_8
//   RV_DCCM_OFFSET, RV_DCCM_RESERVED, RV_DCCM_ROWS, RV_DCCM_SIZE_64
//   RV_DCCM_WIDTH_BITS, RV_DEBUG_SB_MEM, RV_EXTERNAL_DATA, RV_EXTERNAL_DATA_1
//   RV_EXTERNAL_PROG, RV_EXT_ADDRWIDTH, RV_EXT_DATAWIDTH, RV_ICACHE_IC_INDEX
//   RV_ICACHE_IC_ROWS, RV_ICACHE_SIZE, RV_ICACHE_TADDR_HIGH, RV_ICCM_EADR
//   RV_ICCM_NUM_BANKS_8, RV_ICCM_OFFSET, RV_ICCM_RESERVED, RV_ICCM_ROWS
//   RV_ICCM_SIZE_512, RV_LDERR_ROLLBACK, RV_NMI_VEC, RV_NUMIREGS
//   RV_PIC_INT_WORDS, RV_PIC_MEIE_COUNT, RV_PIC_MEIE_MASK, RV_PIC_MEIE_OFFSET
//   RV_PIC_MEIGWCLR_COUNT, RV_PIC_MEIGWCLR_MASK, RV_PIC_MEIGWCLR_OFFSET, RV_PIC_MEIGWCTRL_COUNT
//   RV_PIC_MEIGWCTRL_MASK, RV_PIC_MEIGWCTRL_OFFSET, RV_PIC_MEIPL_COUNT, RV_PIC_MEIPL_MASK
//   RV_PIC_MEIPL_OFFSET, RV_PIC_MEIPT_COUNT, RV_PIC_MEIPT_MASK, RV_PIC_MEIPT_OFFSET
//   RV_PIC_MEIP_COUNT, RV_PIC_MEIP_MASK, RV_PIC_MEIP_OFFSET, RV_PIC_MPICCFG_COUNT
//   RV_PIC_MPICCFG_MASK, RV_PIC_MPICCFG_OFFSET, RV_PIC_OFFSET, RV_RESET_VEC
//   RV_SERIALIO, RV_STERR_ROLLBACK, RV_UNUSED_REGION0, RV_UNUSED_REGION1
//   RV_UNUSED_REGION2, RV_UNUSED_REGION3, RV_UNUSED_REGION4, RV_UNUSED_REGION5
//   RV_UNUSED_REGION6, RV_UNUSED_REGION7, RV_UNUSED_REGION9, RV_XLEN
//   SDVT_AHB
