// `veer_types.sv`, VeeR-EH1's type package: 19 packed structs and one encoded enum.
//
// A SystemVerilog `struct packed` packs its first-declared field at the MSB, and DFHDL's `Struct`
// does the same -- it emits a real `typedef struct packed` into <Top>_defs.svh with the fields in
// declaration order. So field order here is the baseline's, 1:1, with no reversal. That matters:
// several of these packets are sliced as flat bit vectors on their way through the bus and ECC
// paths, so a reordering would be silent until a signature mismatch.
//
// Widths are written as the baseline writes them -- from the named constants in defines.scala, and
// as `HI - LO + 1` where the baseline declared a `[HI:LO]` range -- so the emitted struct keeps the
// names rather than folding to magic numbers.
//
// Two `ifdef branches are resolved by the pinned `default` configuration, which defines neither
// macro: `RV_ICACHE_ECC` (so `icache_err_pkt_t` carries parity, not ECC, and `cache_debug_pkt_t`'s
// write data is 34 bits) and `RV_BTB_48` (so every `way` field is 1 bit).
//
// CAUTION -- fields the baseline declares on a non-zero base. `br_pkt_t.prett` and
// `predict_pkt_t.prett` are `logic [31:1]` upstream and `cache_debug_pkt_t.icache_dicawics` is
// `logic [18:2]`. DFHDL bit ranges are zero-based, so these emit as `[30:0]` and `[16:0]`: the
// same width, packing identically inside the struct, but **indexed differently**. Baseline
// `prett[j]` is `prett(j - 1)` here and `icache_dicawics[j]` is `icache_dicawics(j - 2)`. Each is
// marked at its declaration below. This is an off-by-one that compiles cleanly and would only
// surface as a wrong branch target, so translate those slices deliberately.
//
// Upstream: chipsalliance/Cores-VeeR-EH1@915fb34, via RTLMeter designs/VeeR-EH1.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.veer_eh1

import dfhdl.*

/** `RV_BHT_GHR_RANGE` is `4:0`, a Verilog range fragment rather than a value, so it is not in
  * defines.scala. Its width is `RV_BHT_GHR_SIZE`.
  */
private val GHR_WIDTH: Int <> CONST = RV_BHT_GHR_SIZE

/** The baseline declares `[`RV_BTB_ADDR_HI:`RV_BTB_ADDR_LO]`. */
private val BTB_INDEX_WIDTH: Int <> CONST = RV_BTB_ADDR_HI - RV_BTB_ADDR_LO + 1

// -------------------------------------------------------------------------------------------
// Instruction class, for the performance counters. `Encoded.Manual(4)` rather than a plain
// `Encoded`: the baseline pins `logic [3:0]` explicitly, and a derived width would happen to
// agree today (15 cases -> 4 bits) but would move silently if a case were ever added.
// -------------------------------------------------------------------------------------------

enum inst_t(val value: UInt[4] <> CONST) extends Encoded.Manual(4):
  case NULL extends inst_t(0)
  case MUL extends inst_t(1)
  case LOAD extends inst_t(2)
  case STORE extends inst_t(3)
  case ALU extends inst_t(4)
  case CSRREAD extends inst_t(5)
  case CSRWRITE extends inst_t(6)
  case CSRRW extends inst_t(7)
  case EBREAK extends inst_t(8)
  case ECALL extends inst_t(9)
  case FENCE extends inst_t(10)
  case FENCEI extends inst_t(11)
  case MRET extends inst_t(12)
  case CONDBR extends inst_t(13)
  case JAL extends inst_t(14)
end inst_t

// -------------------------------------------------------------------------------------------
// Trace and debug
// -------------------------------------------------------------------------------------------

case class trace_pkt_t(
    trace_rv_i_valid_ip: Bits[3] <> VAL,
    trace_rv_i_insn_ip: Bits[96] <> VAL,
    trace_rv_i_address_ip: Bits[96] <> VAL,
    trace_rv_i_exception_ip: Bits[3] <> VAL,
    trace_rv_i_ecause_ip: Bits[5] <> VAL,
    trace_rv_i_interrupt_ip: Bits[3] <> VAL,
    trace_rv_i_tval_ip: Bits[32] <> VAL
) extends Struct

/** `RV_ICACHE_ECC` is not defined by the pinned config, so this is the parity variant. */
case class icache_err_pkt_t(
    parity: Bits[8] <> VAL
) extends Struct

/** `icache_wrdata` is `{dicad0[31:0], dicad1[1:0]}`; 42 bits under `RV_ICACHE_ECC`, 34 without. */
case class cache_debug_pkt_t(
    icache_wrdata: Bits[34] <> VAL,
    // baseline `[18:2]`: emits as [16:0], so baseline [j] is icache_dicawics(j - 2) here
    icache_dicawics: Bits[17] <> VAL,
    icache_rd_valid: Bit <> VAL,
    icache_wr_valid: Bit <> VAL
) extends Struct

// -------------------------------------------------------------------------------------------
// Branch prediction
// -------------------------------------------------------------------------------------------

case class br_pkt_t(
    valid: Bit <> VAL,
    toffset: Bits[12] <> VAL,
    hist: Bits[2] <> VAL,
    br_error: Bit <> VAL,
    br_start_error: Bit <> VAL,
    index: Bits[BTB_INDEX_WIDTH.type] <> VAL,
    bank: Bits[2] <> VAL,
    // baseline `[31:1]`: emits as [30:0], so baseline prett[j] is prett(j - 1) here
    prett: Bits[31] <> VAL, // predicted return target
    fghr: Bits[GHR_WIDTH.type] <> VAL,
    way: Bit <> VAL, // 2 bits under `RV_BTB_48`, which the pinned config does not define
    ret: Bit <> VAL,
    btag: Bits[RV_BTB_BTAG_SIZE.type] <> VAL
) extends Struct

case class br_tlu_pkt_t(
    valid: Bit <> VAL,
    hist: Bits[2] <> VAL,
    br_error: Bit <> VAL,
    br_start_error: Bit <> VAL,
    index: Bits[BTB_INDEX_WIDTH.type] <> VAL,
    bank: Bits[2] <> VAL,
    fghr: Bits[GHR_WIDTH.type] <> VAL,
    way: Bit <> VAL,
    middle: Bit <> VAL
) extends Struct

case class predict_pkt_t(
    misp: Bit <> VAL,
    ataken: Bit <> VAL,
    boffset: Bit <> VAL,
    pc4: Bit <> VAL,
    hist: Bits[2] <> VAL,
    toffset: Bits[12] <> VAL,
    index: Bits[BTB_INDEX_WIDTH.type] <> VAL,
    bank: Bits[2] <> VAL,
    valid: Bit <> VAL,
    br_error: Bit <> VAL,
    br_start_error: Bit <> VAL,
    prett: Bits[31] <> VAL, // baseline `[31:1]`: baseline prett[j] is prett(j - 1) here
    pcall: Bit <> VAL,
    pret: Bit <> VAL,
    pja: Bit <> VAL,
    btag: Bits[RV_BTB_BTAG_SIZE.type] <> VAL,
    fghr: Bits[GHR_WIDTH.type] <> VAL,
    way: Bit <> VAL
) extends Struct

case class rets_pkt_t(
    pc0_call: Bit <> VAL,
    pc0_ret: Bit <> VAL,
    pc0_pc4: Bit <> VAL,
    pc1_call: Bit <> VAL,
    pc1_ret: Bit <> VAL,
    pc1_pc4: Bit <> VAL
) extends Struct

// -------------------------------------------------------------------------------------------
// Decode and dispatch
// -------------------------------------------------------------------------------------------

case class trap_pkt_t(
    legal: Bit <> VAL,
    icaf: Bit <> VAL,
    icaf_second: Bit <> VAL,
    perr: Bit <> VAL,
    sbecc: Bit <> VAL,
    fence_i: Bit <> VAL,
    i0trigger: Bits[4] <> VAL,
    i1trigger: Bits[4] <> VAL,
    pmu_i0_itype: inst_t <> VAL,
    pmu_i1_itype: inst_t <> VAL,
    pmu_i0_br_unpred: Bit <> VAL,
    pmu_i1_br_unpred: Bit <> VAL,
    pmu_divide: Bit <> VAL,
    pmu_lsu_misaligned: Bit <> VAL
) extends Struct

case class dest_pkt_t(
    i0rd: Bits[5] <> VAL,
    i0mul: Bit <> VAL,
    i0load: Bit <> VAL,
    i0store: Bit <> VAL,
    i0div: Bit <> VAL,
    i0v: Bit <> VAL,
    i0valid: Bit <> VAL,
    i0secondary: Bit <> VAL,
    i0rs1bype2: Bits[2] <> VAL,
    i0rs2bype2: Bits[2] <> VAL,
    i0rs1bype3: Bits[4] <> VAL,
    i0rs2bype3: Bits[4] <> VAL,
    i1rd: Bits[5] <> VAL,
    i1mul: Bit <> VAL,
    i1load: Bit <> VAL,
    i1store: Bit <> VAL,
    i1v: Bit <> VAL,
    i1valid: Bit <> VAL,
    csrwen: Bit <> VAL,
    csrwonly: Bit <> VAL,
    csrwaddr: Bits[12] <> VAL,
    i1secondary: Bit <> VAL,
    i1rs1bype2: Bits[2] <> VAL,
    i1rs2bype2: Bits[2] <> VAL,
    i1rs1bype3: Bits[7] <> VAL,
    i1rs2bype3: Bits[7] <> VAL
) extends Struct

case class class_pkt_t(
    mul: Bit <> VAL,
    load: Bit <> VAL,
    sec: Bit <> VAL,
    alu: Bit <> VAL
) extends Struct

case class reg_pkt_t(
    rs1: Bits[5] <> VAL,
    rs2: Bits[5] <> VAL,
    rd: Bits[5] <> VAL
) extends Struct

case class dec_pkt_t(
    alu: Bit <> VAL,
    rs1: Bit <> VAL,
    rs2: Bit <> VAL,
    imm12: Bit <> VAL,
    rd: Bit <> VAL,
    shimm5: Bit <> VAL,
    imm20: Bit <> VAL,
    pc: Bit <> VAL,
    load: Bit <> VAL,
    store: Bit <> VAL,
    lsu: Bit <> VAL,
    add: Bit <> VAL,
    sub: Bit <> VAL,
    land: Bit <> VAL,
    lor: Bit <> VAL,
    lxor: Bit <> VAL,
    sll: Bit <> VAL,
    sra: Bit <> VAL,
    srl: Bit <> VAL,
    slt: Bit <> VAL,
    unsign: Bit <> VAL,
    condbr: Bit <> VAL,
    beq: Bit <> VAL,
    bne: Bit <> VAL,
    bge: Bit <> VAL,
    blt: Bit <> VAL,
    jal: Bit <> VAL,
    by: Bit <> VAL,
    half: Bit <> VAL,
    word: Bit <> VAL,
    csr_read: Bit <> VAL,
    csr_clr: Bit <> VAL,
    csr_set: Bit <> VAL,
    csr_write: Bit <> VAL,
    csr_imm: Bit <> VAL,
    presync: Bit <> VAL,
    postsync: Bit <> VAL,
    ebreak: Bit <> VAL,
    ecall: Bit <> VAL,
    mret: Bit <> VAL,
    mul: Bit <> VAL,
    rs1_sign: Bit <> VAL,
    rs2_sign: Bit <> VAL,
    low: Bit <> VAL,
    div: Bit <> VAL,
    rem: Bit <> VAL,
    fence: Bit <> VAL,
    fence_i: Bit <> VAL,
    pm_alu: Bit <> VAL,
    legal: Bit <> VAL
) extends Struct

// -------------------------------------------------------------------------------------------
// Execution units
// -------------------------------------------------------------------------------------------

case class alu_pkt_t(
    valid: Bit <> VAL,
    land: Bit <> VAL,
    lor: Bit <> VAL,
    lxor: Bit <> VAL,
    sll: Bit <> VAL,
    srl: Bit <> VAL,
    sra: Bit <> VAL,
    beq: Bit <> VAL,
    bne: Bit <> VAL,
    blt: Bit <> VAL,
    bge: Bit <> VAL,
    add: Bit <> VAL,
    sub: Bit <> VAL,
    slt: Bit <> VAL,
    unsign: Bit <> VAL,
    jal: Bit <> VAL,
    predict_t: Bit <> VAL,
    predict_nt: Bit <> VAL,
    csr_write: Bit <> VAL,
    csr_imm: Bit <> VAL
) extends Struct

case class mul_pkt_t(
    valid: Bit <> VAL,
    rs1_sign: Bit <> VAL,
    rs2_sign: Bit <> VAL,
    low: Bit <> VAL,
    load_mul_rs1_bypass_e1: Bit <> VAL,
    load_mul_rs2_bypass_e1: Bit <> VAL
) extends Struct

case class div_pkt_t(
    valid: Bit <> VAL,
    unsign: Bit <> VAL,
    rem: Bit <> VAL
) extends Struct

// -------------------------------------------------------------------------------------------
// Load/store
// -------------------------------------------------------------------------------------------

case class lsu_pkt_t(
    by: Bit <> VAL,
    half: Bit <> VAL,
    word: Bit <> VAL,
    dword: Bit <> VAL, // for dma
    load: Bit <> VAL,
    store: Bit <> VAL,
    unsign: Bit <> VAL,
    dma: Bit <> VAL, // dma pkt
    store_data_bypass_c1: Bit <> VAL,
    load_ldst_bypass_c1: Bit <> VAL,
    store_data_bypass_c2: Bit <> VAL,
    store_data_bypass_i0_e2_c2: Bit <> VAL,
    store_data_bypass_e4_c1: Bits[2] <> VAL,
    store_data_bypass_e4_c2: Bits[2] <> VAL,
    store_data_bypass_e4_c3: Bits[2] <> VAL,
    valid: Bit <> VAL
) extends Struct

case class lsu_error_pkt_t(
    exc_valid: Bit <> VAL,
    single_ecc_error: Bit <> VAL,
    inst_type: Bit <> VAL, // 0: load, 1: store
    inst_pipe: Bit <> VAL, // 0: i0,   1: i1
    dma_valid: Bit <> VAL,
    exc_type: Bit <> VAL, // 0: misaligned, 1: access fault
    addr: Bits[32] <> VAL
) extends Struct

case class load_cam_pkt_t(
    valid: Bit <> VAL,
    wb: Bit <> VAL,
    tag: Bits[RV_LSU_NUM_NBLOAD_WIDTH.type] <> VAL,
    rd: Bits[5] <> VAL
) extends Struct

// -------------------------------------------------------------------------------------------
// Triggers
// -------------------------------------------------------------------------------------------

/** `match` is a Scala keyword, so the field is back-ticked to keep the baseline's name. */
case class trigger_pkt_t(
    select: Bit <> VAL,
    `match`: Bit <> VAL,
    store: Bit <> VAL,
    load: Bit <> VAL,
    execute: Bit <> VAL,
    m: Bit <> VAL,
    tdata2: Bits[32] <> VAL
) extends Struct
