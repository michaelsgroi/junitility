package com.michaelsgroi.test.junitility.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.michaelsgroi.test.junitility.runWatcher

class WatchCommand : CliktCommand(name = "watch", help = "Live progress monitor for a running surefire test suite") {
    private val reportsDir by argument(help = "Path to surefire-reports directory")
    private val totalTests by option("--total-tests", help = "Expected total test methods; enables progress bar and ETA")
    private val once by option("--once", help = "Print once and exit (for use with system watch(1))").flag(default = false)

    override fun run() {
        runWatcher(
            reportsDir = reportsDir,
            totalTests = totalTests?.toInt(),
            once = once,
        )
    }
}
