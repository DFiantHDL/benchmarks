// The RTLMeter `hello` workload top. Written fresh for the DFHDL benchmarks repository.
// SPDX-FileCopyrightText: 2026 DFHDL contributors
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** The RTLMeter `hello` workload: prints "Hi, I'm servant!" over the software UART, then loops
  * writing the halt address.
  */
@top class ServantHello extends servant_sim("benchmarks/serv/sw/hello_uart.hex")
