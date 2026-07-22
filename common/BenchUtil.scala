// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks

/** zero-padded lowercase hex, matching the Verilator harness print format */
private[benchmarks] def hex(v: BigInt, digits: Int): String =
  val s = v.toString(16)
  "0".repeat(digits - s.length) + s
