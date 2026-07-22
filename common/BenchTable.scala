// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks

/** Collects one row per benchmarked top and renders a summary comparison table at the end of a run.
  * Each row pairs the DFHDL (DFacsimile Codegen) throughput with the Verilator reference (when
  * `--verilator` is set) and cross-checks the architectural signature the two simulators produced.
  *
  * A standalone bench main calls [[flush]] at its end to print and clear its own table;
  * [[benchRun]] calls [[defer]] first so the three suites accumulate into one combined table it
  * prints via [[printAndClear]].
  */
object BenchTable:
  private final case class Row(
      label: String,
      dfMcps: Double,
      dfSig: String,
      vMcps: Option[Double],
      vSig: Option[String]
  )
  private val rows = scala.collection.mutable.ArrayBuffer.empty[Row]
  private var deferred = false

  /** Extract `key=<hex>` from a printed state line. Both the DFacsimile state line and the
    * Verilator harness print the architectural signature as `key=hexdigits`, so this cross-checks
    * them.
    */
  def field(key: String, text: String): String =
    s"""\\b${java.util.regex.Pattern.quote(key)}=([0-9a-fA-Fx]+)""".r
      .findFirstMatchIn(text).map(_.group(1)).getOrElse("")

  def add(
      label: String,
      dfMcps: Double,
      dfSig: String,
      vMcps: Option[Double],
      vSig: Option[String]
  ): Unit = rows += Row(label, dfMcps, dfSig, vMcps, vSig)

  /** Suppress per-main printing so several suites can accumulate into one table (used by benchRun).
    */
  def defer(): Unit = deferred = true

  /** Print and clear unless printing was deferred to an outer orchestrator. */
  def flush(): Unit = if !deferred then printAndClear()

  def printAndClear(): Unit =
    if rows.nonEmpty then render()
    rows.clear()
    deferred = false

  private def render(): Unit =
    val anyV = rows.exists(_.vMcps.isDefined)
    val header =
      if anyV then Vector("Benchmark", "DFHDL Mcps", "Verilator Mcps", "DF / Veri", "signature")
      else Vector("Benchmark", "DFHDL Mcps")
    val body = rows.toVector.map { r =>
      val df = f"${r.dfMcps}%.2f"
      if anyV then
        val v = r.vMcps.map(m => f"$m%.2f").getOrElse("-")
        val speed = r.vMcps.map(m => f"${r.dfMcps / m}%.2fx").getOrElse("-")
        val sig = (r.vSig, r.vMcps) match
          case (Some(vs), _) if r.dfSig.nonEmpty && vs.equalsIgnoreCase(r.dfSig) => "match"
          case (Some(_), _)                                                      => "DIFF"
          case _                                                                 => "-"
        Vector(r.label, df, v, speed, sig)
      else Vector(r.label, df)
    }
    val rightAlign = if anyV then Vector(false, true, true, true, false) else Vector(false, true)
    val widths = header.indices.map { c =>
      (header(c) +: body.map(_(c))).map(_.length).max
    }
    def line(cells: Vector[String]): String =
      cells.indices.map { c =>
        val w = widths(c)
        val s = cells(c)
        val pad = " " * (w - s.length)
        if rightAlign(c) then pad + s else s + pad
      }.mkString("| ", " | ", " |")
    val sep = widths.map(w => "-" * (w + 2)).mkString("+", "+", "+")
    println()
    println(sep)
    println(line(header))
    println(sep)
    body.foreach(r => println(line(r)))
    println(sep)
    if anyV then
      println("DF / Veri = DFHDL Codegen throughput / Verilator throughput; " +
        "signature = architectural state cross-check.")
  end render
end BenchTable
