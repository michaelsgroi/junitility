package com.michaelsgroi.test.junitility.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.michaelsgroi.test.junitility.JunitilityCommand
import com.michaelsgroi.test.junitility.core.JsonGenerator
import com.michaelsgroi.test.junitility.core.XmlParser
import java.io.File

class JsonCommand : CliktCommand(name = "json", help = "Generate JSON summary from TEST-*.xml files") {
    private val inputDir by argument(help = "Directory containing TEST-*.xml files")
    private val outputFile by argument(help = "Output JSON file path")
    private val csvPath by option("--csv-path", help = "Path to reference in detailedReportPath field").default("test-results.csv")

    override fun run() {
        val debug = (currentContext.parent?.command as? JunitilityCommand)?.debug ?: false

        val results = XmlParser.parseDirectory(File(inputDir), debug) { msg -> if (debug) echo(msg) }

        val jsonFile = File(outputFile)
        JsonGenerator.generate(results, jsonFile, csvPath)

        echo("Generated: ${jsonFile.absoluteFile.relativeTo(File(".").absoluteFile)}")
    }
}
