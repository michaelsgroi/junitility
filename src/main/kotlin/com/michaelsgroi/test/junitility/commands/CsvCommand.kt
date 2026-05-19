package com.michaelsgroi.test.junitility.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.michaelsgroi.test.junitility.JunitilityCommand
import com.michaelsgroi.test.junitility.core.CsvGenerator
import com.michaelsgroi.test.junitility.core.XmlParser
import java.io.File

class CsvCommand : CliktCommand(name = "csv", help = "Generate CSV report from TEST-*.xml files") {
    private val inputDir by argument(help = "Directory containing TEST-*.xml files")
    private val outputFile by argument(help = "Output CSV file path")

    override fun run() {
        val debug = (currentContext.parent?.command as? JunitilityCommand)?.debug ?: false

        val results = XmlParser.parseDirectory(File(inputDir), debug) { msg -> if (debug) echo(msg) }

        val csvFile = File(outputFile)
        CsvGenerator.generate(results, csvFile)

        echo("Generated: ${csvFile.absoluteFile.relativeTo(File(".").absoluteFile)}")
    }
}
