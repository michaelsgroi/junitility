package com.michaelsgroi.test.junitility.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.michaelsgroi.test.junitility.core.InputFormat
import com.michaelsgroi.test.junitility.core.NetImpactGenerator
import com.michaelsgroi.test.junitility.core.TestResultsParser
import java.io.File

class CompareCommand : CliktCommand(name = "compare", help = "Generate net impact reports from test results") {
    private val baselineDir by argument(name = "baseline-dir")
    private val patchedDir by argument(name = "patched-dir")
    private val outputDir by option("--output").required()
    private val format by option("--format", help = "Input format: auto, surefire, or allure").default("auto")

    override fun run() {
        val baseline = File(baselineDir)
        val patched = File(patchedDir)
        val output = File(outputDir)

        val inputFormat = InputFormat.valueOf(format.uppercase())
        val baselineResults = TestResultsParser.parseDirectory(baseline, inputFormat)
        val patchedResults = TestResultsParser.parseDirectory(patched, inputFormat)

        if (output.exists()) {
            output.deleteRecursively()
        }
        output.mkdirs()

        val netImpact = NetImpactGenerator.compare(baselineResults, patchedResults)

        val summaryFile = File(output, "pr-impact-summary.md")
        NetImpactGenerator.generateSummaryMarkdown(netImpact, summaryFile)
        echo("Generated: ${summaryFile.absoluteFile.relativeTo(File(".").absoluteFile)}")

        val csvFile = File(output, "pr-impact-details.csv")
        NetImpactGenerator.generateDetailedCsv(netImpact, csvFile)
        echo("Generated: ${csvFile.absoluteFile.relativeTo(File(".").absoluteFile)}")
    }
}
