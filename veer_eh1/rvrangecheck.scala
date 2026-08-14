// `beh_lib.sv`'s address range checker: is `addr` inside a CCM window, and is it in its region?
//
// Purely combinational, so it carries no clock or reset.
//
// Instantiated 8 times across dma_ctrl, ifu_ifc_ctl and lsu_addrcheck, with `CCM_SADR`/`CCM_SIZE`
// bound to the DCCM, ICCM and PIC windows from defines.scala. It stays a single generic module.
//
// Upstream: chipsalliance/Cores-VeeR-EH1@915fb34, via RTLMeter designs/VeeR-EH1.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks.veer_eh1

import dfhdl.*

class rvrangecheck(
    val CCM_SADR: Bits[32] <> CONST = all(0),
    val CCM_SIZE: Int <> CONST      = 128
) extends RTDesign:
  val addr      = Bits(32) <> IN // address to be checked for range
  val in_range  = Bit      <> OUT // S_ADDR <= addr < E_ADDR
  val in_region = Bit      <> OUT

  val REGION_BITS: Int <> CONST = 4
  val MASK_BITS: Int <> CONST   = 10 + clog2(CCM_SIZE)

  // the baseline's `start_addr` and `region` are pure renames of `CCM_SADR`
  in_region <> addr(31, 32 - REGION_BITS) == CCM_SADR(31, 32 - REGION_BITS)

  // A 48 KB CCM is not a power of two, so its top quarter is masked off. The baseline selects that
  // with a `generate`; a `.sel` on the constant condition folds the same way without pinning
  // CCM_SIZE.
  in_range <> (addr(31, MASK_BITS) == CCM_SADR(31, MASK_BITS)) &
    (CCM_SIZE == 48).sel(~(addr(MASK_BITS - 1, MASK_BITS - 2).&), 1)
end rvrangecheck
