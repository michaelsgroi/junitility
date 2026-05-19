package com.michaelsgroi.test.junitility.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.michaelsgroi.test.junitility.JunitilityCommand
import com.michaelsgroi.test.junitility.core.CsvGenerator
import com.michaelsgroi.test.junitility.core.FileUtils
import com.michaelsgroi.test.junitility.core.JsonGenerator
import com.michaelsgroi.test.junitility.core.XmlParser
import java.io.File
import kotlin.system.exitProcess

class ReportCommand : CliktCommand(name = "report", help = "Generate full report (copy files + CSV + JSON)") {
    private val inputDir by argument(help = "Directory containing TEST-*.xml files")
    private val label by argument(help = "Label for this test run (used as subdirectory name)")
    private val reportsDir by option("--reports-dir", help = "Output directory").default("reports")

    override fun run() {
        val debug = (currentContext.parent?.command as? JunitilityCommand)?.debug ?: false
        val input = File(inputDir)

        if (!input.exists() || !input.isDirectory) {
            System.err.println("Error: ${input.path} is not a valid directory")
            exitProcess(2)
        }

        echo("Parsing...")
        val outputDir = File(reportsDir, label)

        if (outputDir.exists()) {
            if (!FileUtils.promptOverwrite(outputDir)) {
                exitProcess(1)
            }
            if (debug) echo("Deleting existing directory: ${outputDir.path}")
            outputDir.deleteRecursively()
        }

        if (debug) echo("Copying ${input.path} to ${outputDir.path}")
        FileUtils.copyDirectory(input, outputDir)

        val results = XmlParser.parseDirectory(outputDir, debug) { msg -> if (debug) echo(msg) }

        echo("Generating CSV...")
        CsvGenerator.generate(results, File(outputDir, "test-results.csv"))

        echo("Generating JSON...")
        JsonGenerator.generate(results, File(outputDir, "test-summary.json"), "test-results.csv")

        echo("Done.")
    }
}
