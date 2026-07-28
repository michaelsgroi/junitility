package com.michaelsgroi.test.junitility

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger
import org.apache.maven.plugins.surefire.report.TestSuiteXmlParser
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

fun main(args: Array<String>) {
    fun named(name: String) = args.firstOrNull { it.startsWith("--$name=") }?.substringAfter('=')

    val once = "--once" in args

    val reportsDir =
        named("reports-dir") ?: System.getenv("REPORTS_DIR") ?: run {
            System.err.println("Usage: surefire-watcher --reports-dir=<path> [--total-tests=<n>] [--once]")
            System.err.println("  --reports-dir  path to surefire-reports directory")
            System.err.println("  --total-tests  expected total number of test methods; enables progress bar and ETA (optional)")
            System.err.println("  --once         print current state once and exit (for use with system watch(1))")
            System.exit(1)
            return
        }

    val totalTests = named("total-tests")?.toIntOrNull() ?: System.getenv("TOTAL_TESTS")?.toIntOrNull()

    runWatcher(reportsDir = reportsDir, totalTests = totalTests, once = once)
}

fun runWatcher(
    reportsDir: String,
    totalTests: Int? = null,
    once: Boolean,
) {
    val dir = File(reportsDir)
    if (!dir.exists() || !dir.isDirectory) {
        System.err.println("Error: $reportsDir is not a valid directory")
        System.exit(2)
        return
    }

    val startWallMs = System.currentTimeMillis()
    val parser = TestSuiteXmlParser(silentLogger)
    val firstSeenMs = mutableMapOf<String, Long>()

    while (true) {
        val now = System.currentTimeMillis()

        val allXmls =
            dir.listFiles { f ->
                f.isFile && f.name.startsWith("TEST-") && f.name.endsWith(".xml")
            } ?: emptyArray()

        for (f in allXmls) {
            firstSeenMs.getOrPut(f.name) { now }
        }

        var passed = 0
        var failed = 0
        var errors = 0
        var skipped = 0
        var lastClass = ""
        var lastClassMs = 0L
        val failures = mutableListOf<String>()
        val inProgress = mutableListOf<Pair<String, Long>>()

        for (file in allXmls.sortedByDescending { it.lastModified() }) {
            try {
                val suites = parser.parse(file.absolutePath)
                if (suites.isEmpty()) {
                    inProgress.add(classNameFromFile(file) to (firstSeenMs[file.name] ?: now))
                    continue
                }
                for (suite in suites) {
                    if (suite.testCases.isNullOrEmpty()) {
                        inProgress.add(classNameFromFile(file) to (firstSeenMs[file.name] ?: now))
                        continue
                    }
                    if (file.lastModified() > lastClassMs) {
                        lastClassMs = file.lastModified()
                        lastClass = suite.name?.substringAfterLast('.') ?: classNameFromFile(file)
                    }
                    for (tc in suite.testCases) {
                        when {
                            tc.isSuccessful -> {
                                passed++
                            }

                            tc.hasFailure() -> {
                                failed++
                                failures.add("${tc.fullClassName}#${tc.name}")
                            }

                            tc.hasError() -> {
                                errors++
                                failures.add("${tc.fullClassName}#${tc.name}")
                            }

                            tc.hasSkipped() -> {
                                skipped++
                            }

                            else -> {
                                errors++
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                inProgress.add(classNameFromFile(file) to (firstSeenMs[file.name] ?: now))
            }
        }

        val completedCount = allXmls.size - inProgress.size
        val elapsedMs = now - startWallMs
        val completedTests = passed + failed + errors + skipped

        val firstCompletedMs =
            allXmls
                .filter { it !in inProgress.map { p -> File(dir, "TEST-${p.first}.xml") } }
                .minOfOrNull { it.lastModified() }

        val etaStr =
            if (totalTests != null && completedTests > 0 && firstCompletedMs != null) {
                val sinceFirstMs = (now - firstCompletedMs).coerceAtLeast(1)
                val msPerTest = sinceFirstMs.toDouble() / completedTests
                val remaining = (totalTests - completedTests).coerceAtLeast(0)
                formatDuration((remaining * msPerTest).toLong())
            } else {
                "--:--"
            }

        val progressLine =
            if (totalTests != null) {
                val pct = (completedTests * 100.0 / totalTests).roundToInt()
                val bar = progressBar(completedTests, totalTests, width = 25)
                "$bar  $completedTests/$totalTests tests ($pct%)"
            } else {
                "$completedCount classes done"
            }

        println("Watching: $reportsDir  |  Elapsed: ${formatDuration(elapsedMs)}")
        println()
        println(progressLine)
        println()
        println("  passed:  $passed")
        println("  failed:  $failed")
        println("  errors:  $errors")
        println("  skipped: $skipped")
        println()
        println("  eta:     $etaStr")
        if (lastClass.isNotEmpty()) {
            println("  last:    $lastClass (${formatDuration(now - lastClassMs)} ago)")
        }

        if (inProgress.isNotEmpty()) {
            println()
            println("Running (${inProgress.size}):")
            for ((name, seenMs) in inProgress.take(5)) {
                println("  > $name  ${formatDuration(now - seenMs)}")
            }
            if (inProgress.size > 5) println("  ... and ${inProgress.size - 5} more")
        }

        if (failures.isNotEmpty()) {
            println()
            println("Failures/Errors (${failures.size}):")
            failures.take(10).forEach { println("  - $it") }
            if (failures.size > 10) println("  ... and ${failures.size - 10} more")
        }

        System.out.flush()

        val done = totalTests != null && completedTests >= totalTests
        if (once || done) {
            if (done) {
                println()
                println("Done. $completedTests tests complete in ${formatDuration(elapsedMs)}.")
            }
            break
        }

        Thread.sleep(2_000)
    }
}

internal fun classNameFromFile(file: File): String = file.nameWithoutExtension.removePrefix("TEST-").substringAfterLast('.')

internal val silentLogger =
    object : ConsoleLogger {
        override fun debug(message: String?) {}

        override fun info(message: String?) {}

        override fun warning(message: String?) {}

        override fun error(message: String?) {}

        override fun error(
            message: String?,
            throwable: Throwable?,
        ) {}

        override fun error(throwable: Throwable?) {}

        override fun isDebugEnabled() = false

        override fun isInfoEnabled() = false

        override fun isWarnEnabled() = false

        override fun isErrorEnabled() = false
    }

internal fun progressBar(
    done: Int,
    total: Int,
    width: Int,
): String {
    val fill = if (total > 0) (done * width / total).coerceIn(0, width) else 0
    return "[" + "#".repeat(fill) + "-".repeat(width - fill) + "]"
}

internal fun formatDuration(ms: Long): String {
    val totalSec = TimeUnit.MILLISECONDS.toSeconds(ms)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
