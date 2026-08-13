// `beh_lib.sv`'s address range checker: is `addr` inside a CCM window, and is it in its region?
//
// Purely combinational, so it carries no clock or reset.
//
// Instantiated 8 times across dma_ctrl, ifu_ifc_ctl and lsu_addrcheck, with `CCM_SADR`/`CCM_SIZE`
// bound to the DCCM, ICCM and PIC windows from defines.scala. It stays a single generic module,
// which needs the elaboration never to *read* either parameter -- see the two notes below.
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

  // The baseline names `CCM_SADR` as `start_addr` and slices that, and names its top bits
  // `region`. Both are pure renames, so the parameter is sliced directly rather than through
  // intermediate values. That also steps around DFHDL#484, where a VAR read through a
  // parameter-bounded slice is misreported as a latch.
  in_region <> addr(31, 32 - REGION_BITS) == CCM_SADR(31, 32 - REGION_BITS)

  // A 48 KB CCM is not a power of two, so its top quarter is masked off. The baseline selects that
  // with a `generate` on the parameter; here it is a `.sel` on a constant condition, which folds
  // the same way without the elaboration ever *reading* CCM_SIZE -- so the design keeps the extra
  // term and stays one generic module rather than a specialised copy per instantiation.
  in_range <> (addr(31, MASK_BITS) == CCM_SADR(31, MASK_BITS)) &
    (CCM_SIZE == 48).sel(~(addr(MASK_BITS - 1, MASK_BITS - 2).&), 1)
end rvrangecheck
