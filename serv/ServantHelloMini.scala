// The hello workload on a small RAM. Written fresh for the DFHDL benchmarks repository.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** The hello workload on a 256-byte RAM (the firmware only touches the bottom 96 bytes): small
  * enough for DFacsimile's current packed-bits lowering of mutable memories.
  */
@top class ServantHelloMini extends servant_sim("benchmarks/serv/sw/hello_uart.hex", memsize = 256)
