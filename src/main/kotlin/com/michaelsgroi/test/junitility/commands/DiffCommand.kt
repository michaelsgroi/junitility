package com.michaelsgroi.test.junitility.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.michaelsgroi.test.junitility.core.CsvDiffer
import java.io.File
import kotlin.system.exitProcess

class DiffCommand : CliktCommand(name = "diff", help = "Compare test results from two directories") {
    private val dir1 by argument(help = "First directory containing test-results.csv")
    private val dir2 by argument(help = "Second directory containing test-results.csv")

    override fun run() {
        val firstDir = File(dir1)
        val secondDir = File(dir2)

        if (!firstDir.exists() || !firstDir.isDirectory) {
            System.err.println("Error: ${firstDir.path} is not a valid directory")
            exitProcess(2)
        }

        if (!secondDir.exists() || !secondDir.isDirectory) {
            System.err.println("Error: ${secondDir.path} is not a valid directory")
            exitProcess(2)
        }

        val csv1 = File(firstDir, "test-results.csv")
        val csv2 = File(secondDir, "test-results.csv")

        if (!csv1.exists()) {
            System.err.println("Error: ${csv1.path} does not exist")
            exitProcess(2)
        }

        if (!csv2.exists()) {
            System.err.println("Error: ${csv2.path} does not exist")
            exitProcess(2)
        }

        val outputFileName = "${firstDir.name}-${secondDir.name}.csv"
        val outputFile = File(outputFileName)

        CsvDiffer.generateDiff(csv1, csv2, outputFile, firstDir.name, secondDir.name)

        echo("Generated: ${outputFile.absoluteFile.relativeTo(File(".").absoluteFile)}")
    }
}
