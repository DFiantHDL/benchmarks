// SPDX-License-Identifier: Apache-2.0
package dfhdl.benchmarks

/** Builds and runs the external Verilator model for a committed benchmark top, so `--verilator` can
  * print the Verilog reference measurement next to the DFacsimile one.
  *
  * Expects the top's generated SystemVerilog under `sandbox/<top>/hdl` (produced by the bench's
  * `.compile` step) and a C++ harness that verilates with `--prefix VTOP` and takes
  * `<warmup> <timed>` cycle arguments (the `bench_*.cpp` under each benchmark's `verilator/`). The
  * model is built once per top per JVM run and reused across configs.
  *
  * Verilator discovery: the binary is `verilator` on Unix and `verilator_bin.exe` on Windows (the
  * perl `verilator` wrapper is broken outside the OSS CAD Suite env scripts), overridable with the
  * `VERILATOR_BIN` env var; both are resolved through `PATH`. On Windows the generated makefile's
  * back-slashed `VERILATOR_ROOT` gets stripped by the shell in the include step, so a
  * forward-slashed `VERILATOR_ROOT` is passed explicitly (from the env var, or derived from the
  * binary location).
  */
object Verilator:
  private val built = scala.collection.mutable.Set.empty[String]
  private val skip = scala.collection.mutable.Set.empty[String]
  private val isWindows = sys.props.getOrElse("os.name", "").toLowerCase.contains("win")

  /** Runtime thread count for the verilated model: 0/1 build the classic single-threaded model (no
    * `--threads` flag, the default reference), >= 2 verilate with `--threads N` so Verilator
    * partitions the design into MTasks across an internal thread pool. Set once per JVM run via
    * [[configure]] from a `--threads N` / `--threads=N` CLI argument.
    */
  var threads: Int = 0

  /** Parse `--threads N` (or `--threads=N`) out of a benchmark main's args and set [[threads]].
    * Absent or < 2 leaves the single-threaded reference build unchanged.
    */
  def configure(args: Array[String]): Unit =
    val i = args.indexOf("--threads")
    val n =
      if i >= 0 && i + 1 < args.length then args(i + 1).toIntOption.getOrElse(0)
      else args.collectFirst { case s if s.startsWith("--threads=") => s.drop(10).toIntOption }
        .flatten.getOrElse(0)
    threads = n
    if threads >= 2 then println(s"[verilator] multithreaded model: --threads $threads")

  // cache builds per (top, thread count) so switching --threads within a run rebuilds
  private def key(top: String): String = s"$top#${math.max(threads, 1)}"

  private lazy val binaryName: String =
    sys.env.getOrElse("VERILATOR_BIN", if isWindows then "verilator_bin.exe" else "verilator")

  private lazy val rootEnv: Map[String, String] =
    sys.env.get("VERILATOR_ROOT") match
      case Some(r)           => Map("VERILATOR_ROOT" -> r.replace('\\', '/'))
      case None if isWindows =>
        // derive <root>/share/verilator from <root>/bin/verilator_bin.exe (via `where`)
        scala.util.Try {
          val line =
            os.proc("where", binaryName).call(check = false).out.trim().linesIterator.next()
          val root = os.Path(line) / os.up / os.up / "share" / "verilator"
          Map("VERILATOR_ROOT" -> root.toString.replace('\\', '/'))
        }.getOrElse(Map.empty)
      case None => Map.empty

  private val mcpsRe = """=\s*([0-9.]+)\s*Mcycles/s""".r

  private def parse(out: String): Option[(Double, String)] =
    val mcps = mcpsRe.findFirstMatchIn(out).map(_.group(1).toDouble)
    val state = out.linesIterator.find(_.contains("after ")).getOrElse("")
    mcps.map(m => (m, state))

  /** Build (once) and run `sandbox/<top>/hdl` under `harnessRel` for `warmup`+`timed` cycles,
    * echoing the harness output and returning its `(Mcycles/s, state line)`. A missing HDL dir or a
    * failed build is reported once, then skipped on later configs for the same top, returning None.
    */
  def run(top: String, harnessRel: String, warmup: Long, timed: Long): Option[(Double, String)] =
    val hdlDir = os.pwd / "sandbox" / top / "hdl"
    val k = key(top)
    if skip.contains(k) then None
    else if !os.exists(hdlDir) then
      println(s"[verilator] $top: no HDL at $hdlDir")
      skip += k
      None
    else
      val ready = built.contains(k) || build(top, hdlDir, harnessRel)
      if !ready then
        skip += k
        None
      else
        built += k
        val exe = hdlDir / "obj_dir" / (if isWindows then "VTOP.exe" else "VTOP")
        val res = os.proc(exe.toString, warmup.toString, timed.toString)
          .call(cwd = hdlDir, stdout = os.Pipe, stderr = os.Pipe, check = false)
        val out = res.out.text() + res.err.text()
        print(out) // echo the harness measurement live
        parse(out)
    end if
  end run

  private def build(top: String, hdlDir: os.Path, harnessRel: String): Boolean =
    try
      os.remove.all(hdlDir / "obj_dir")
      val svs = os.list(hdlDir).filter(_.ext == "sv").map(_.toString).toSeq
      val harness = (os.pwd / os.RelPath(harnessRel)).toString
      // >= 2 threads: verilate a multithreaded model (MTask partitioning + internal thread pool).
      // The C++ harness API (construct + eval) is unchanged; Verilator manages the pool and the
      // generated makefile links pthread. 0/1 leaves the classic single-threaded reference.
      val threadFlags = if threads >= 2 then Seq("--threads", threads.toString) else Seq.empty
      val cmd = Seq(
        binaryName,
        "-O3",
        "--cc",
        "--exe",
        "--build",
        "-j",
        "0",
        "--prefix",
        "VTOP",
        "-Mdir",
        "obj_dir",
        "-Wno-fatal",
        "--top-module",
        top,
        s"-I${hdlDir}"
      ) ++ threadFlags ++ svs ++
        Seq(harness, "-CFLAGS", "-O3")
      val res =
        os.proc(cmd).call(cwd = hdlDir, env = rootEnv, check = false, stdout = os.Pipe,
          stderr = os.Pipe)
      if res.exitCode == 0 then true
      else
        println(s"[verilator] build failed for $top (exit ${res.exitCode}):")
        println((res.out.text() + res.err.text()).linesIterator.toSeq.takeRight(12).mkString("\n"))
        false
    catch
      case e: Throwable =>
        println(
          s"[verilator] could not run '$binaryName' for $top (set VERILATOR_BIN / add it to " +
            s"PATH): ${e.getMessage}"
        )
        false
  end build
end Verilator
