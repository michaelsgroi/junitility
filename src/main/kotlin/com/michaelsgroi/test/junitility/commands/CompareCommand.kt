package com.michaelsgroi.test.junitility.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.michaelsgroi.test.junitility.core.NetImpactGenerator
import com.michaelsgroi.test.junitility.core.XmlParser
import java.io.File
import kotlin.system.exitProcess

class CompareCommand : CliktCommand(name = "compare", help = "Generate net impact reports from test results") {
    private val baselineDir by argument(name = "baseline-dir")
    private val patchedDir by argument(name = "patched-dir")
    private val outputDir by option("--output").required()

    override fun run() {
        val baseline = File(baselineDir)
        val patched = File(patchedDir)
        val output = File(outputDir)

        validateInputDirectory(baseline)
        validateInputDirectory(patched)

        val baselineResults = XmlParser.parseDirectory(baseline)
        val patchedResults = XmlParser.parseDirectory(patched)

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

    private fun validateInputDirectory(dir: File) {
        if (!dir.exists() || !dir.isDirectory) {
            System.err.println("Error: ${dir.path} is not a valid directory")
            exitProcess(2)
        }
        val xmlFiles = dir.listFiles { file -> file.extension == "xml" } ?: emptyArray()
        if (xmlFiles.isEmpty()) {
            System.err.println("Error: No XML files found in ${dir.path}")
            exitProcess(2)
        }
    }
}
