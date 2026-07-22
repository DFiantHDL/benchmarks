// The RTLMeter `phil` workload top. Written fresh for the DFHDL benchmarks repository.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** The RTLMeter `phil` workload: Zephyr dining philosophers (timer interrupts, CSRs, full 32 KiB
  * RAM).
  */
@top class ServantPhil extends servant_sim("benchmarks/serv/sw/zephyr_phil.hex")
