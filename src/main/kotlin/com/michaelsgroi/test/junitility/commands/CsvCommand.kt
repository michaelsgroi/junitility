package com.michaelsgroi.test.junitility.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.michaelsgroi.test.junitility.JunitilityCommand
import com.michaelsgroi.test.junitility.core.CsvGenerator
import com.michaelsgroi.test.junitility.core.InputFormat
import com.michaelsgroi.test.junitility.core.TestResultsParser
import java.io.File

class CsvCommand : CliktCommand(name = "csv", help = "Generate CSV report from Surefire/Failsafe XML or Allure results") {
    private val inputDir by argument(help = "Directory containing Surefire/Failsafe XML or Allure results")
    private val outputFile by argument(help = "Output CSV file path")
    private val format by option("--format", help = "Input format: auto, surefire, or allure").default("auto")

    override fun run() {
        val debug = (currentContext.parent?.command as? JunitilityCommand)?.debug ?: false

        val results = TestResultsParser.parseDirectory(File(inputDir), format.toInputFormat(), debug) { msg -> if (debug) echo(msg) }

        val csvFile = File(outputFile)
        CsvGenerator.generate(results, csvFile)

        echo("Generated: ${csvFile.absoluteFile.relativeTo(File(".").absoluteFile)}")
    }

    private fun String.toInputFormat(): InputFormat = InputFormat.valueOf(uppercase())
}
