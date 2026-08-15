// `dec_gpr_ctl.sv`: the architectural register file, 31 x 32-bit GPRs.
//
// Four read ports (two per issue slot) and three write ports (i0, i1, and the non-blocking load
// writeback). x0 is not a register, so the array is the baseline's `[31:1]`.
//
// One departure from the baseline, forced: `gpr_out`/`gpr_in` are declared `[31:1]` there, and a
// DFHDL `Vec` is 0-based, so the baseline's index `j` reaches them as `j - 1`. Everything else
// (`w0v`, `gpr_wr_en`, ...) keeps the baseline's base-1 numbering through `BitsHL`.
//
// `active_clk` is a real port, declared as a derived clock (see `active` below). Nothing in the
// pinned build drives it, so it collapses onto the root clock at every parent (`.active_clk(clk)`),
// which is what `rvoclkhdr`'s `assign l1clk = clk` does under `RV_FPGA_OPTIMIZE`.
//
// `GPR_BANKS` is read by the bank loop, so it is pinned rather than generic. The sole instantiation
// (dec.sv:521) passes 1, and at one bank the baseline's two spellings of the bank compare,
// `gpr_bank_id == i` in the write path and `gpr_bank_id == 1'(i)` in the read path, coincide.
//
// Upstream: chipsalliance/Cores-VeeR-EH1@915fb34, via RTLMeter designs/VeeR-EH1.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.veer_eh1

import dfhdl.*

class dec_gpr_ctl(
    val GPR_BANKS: Int <> CONST = 1
) extends RTDesign:
  // dec.sv:406, verbatim. NOT a plain `clog2`: `$clog2(1)` is 0, which would make `wr_bank_id`
  // and `gpr_bank_id` zero-width, so the ternary floors it at 1.
  val GPR_BANKS_LOG2: Int <> CONST = (GPR_BANKS == 1).sel(1, clog2(GPR_BANKS))

  val raddr0 = Bits(5) <> IN // logical read addresses
  val raddr1 = Bits(5) <> IN
  val raddr2 = Bits(5) <> IN
  val raddr3 = Bits(5) <> IN

  val rden0 = Bit <> IN // read enables
  val rden1 = Bit <> IN
  val rden2 = Bit <> IN
  val rden3 = Bit <> IN

  val waddr0 = Bits(5) <> IN // logical write addresses
  val waddr1 = Bits(5) <> IN
  val waddr2 = Bits(5) <> IN

  val wen0 = Bit <> IN // write enables
  val wen1 = Bit <> IN
  val wen2 = Bit <> IN

  val wd0 = Bits(32) <> IN // write data
  val wd1 = Bits(32) <> IN
  val wd2 = Bits(32) <> IN

  val wen_bank_id = Bit                  <> IN // write enable for banks
  val wr_bank_id  = Bits(GPR_BANKS_LOG2) <> IN // read enable for banks

  val rd0 = Bits(32) <> OUT // read data
  val rd1 = Bits(32) <> OUT
  val rd2 = Bits(32) <> OUT
  val rd3 = Bits(32) <> OUT

  val scan_mode = Bit <> IN

  val gpr_out = Bits(32) X 31 X GPR_BANKS <> VAR.REG init all(all(all(0))) // 31 x 32 bit GPRs
  val gpr_in  = Bits(32) X 31             <> VAR
  val w0v     = BitsHL(31, 1)             <> VAR
  val w1v     = BitsHL(31, 1)             <> VAR
  val w2v     = BitsHL(31, 1)             <> VAR

  // `bankid_ff` is an `rvdffs`, so the baseline puts it on the gated `active_clk` while the
  // self-gating `rvdffe` GPRs stay on the root `clk`. `active` declares the derived clock port;
  // the region opens that context around the one flop, at the baseline's own declaration site.
  val active = new RTDerivedClkDomain {}

  val bankid = new active.RTRegion:
    val gpr_bank_id = Bits(GPR_BANKS_LOG2) <> VAR.REG init all(0)
    if (wen_bank_id) gpr_bank_id.din := wr_bank_id
  import bankid.gpr_bank_id // the region has no naming footprint; keep the body reading as the gold

  // GPR Write logic
  for (j <- 1 until 32)
    w0v(j)        <> wen0 & (waddr0.uint == j)
    w1v(j)        <> wen1 & (waddr1.uint == j)
    w2v(j)        <> wen2 & (waddr2.uint == j)
    gpr_in(j - 1) <> (w0v(j).repeat(32) & wd0) | (w1v(j).repeat(32) & wd1) |
      (w2v(j).repeat(32) & wd2)

  // GPR Write Enables for power savings
  val gpr_wr_en: BitsHL[31, 1] <> VAL = w0v | w1v | w2v
  val gpr_bank_wr_en                  = BitsHL(31, 1) X GPR_BANKS <> VAR
  for (i <- 0 until GPR_BANKS)
    gpr_bank_wr_en(i) <> gpr_wr_en & (gpr_bank_id.uint == i).repeat(31)
    for (j <- 1 until 32)
      if (gpr_bank_wr_en(i)(j)) gpr_out(i)(j - 1).din := gpr_in(j - 1)

  // GPR Read logic
  @inline def readPort(rden: Bit <> VAL, raddr: Bits[5] <> VAL): Bits[32] <> DFRET =
    (for (i <- 0 until GPR_BANKS; j <- 1 until 32)
      yield (rden & (raddr.uint == j) & (gpr_bank_id.uint == i)).repeat(32) & gpr_out(i)(j - 1))
      .reduce[Bits[32] <> VAL](_ | _)

  rd0 <> readPort(rden0, raddr0)
  rd1 <> readPort(rden1, raddr1)
  rd2 <> readPort(rden2, raddr2)
  rd3 <> readPort(rden3, raddr3)
end dec_gpr_ctl
