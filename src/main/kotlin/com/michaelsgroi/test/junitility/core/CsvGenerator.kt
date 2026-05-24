package com.michaelsgroi.test.junitility.core

import com.michaelsgroi.test.junitility.model.TestResult
import java.io.File
import kotlin.system.exitProcess

object CsvGenerator {
    fun generate(
        results: List<TestResult>,
        outputFile: File,
    ) {
        try {
            outputFile.parentFile?.mkdirs()
            outputFile.bufferedWriter().use { writer ->
                writer.write(CsvQuoting.writeCsvRow(listOf("ClassName", "MethodName", "Outcome")))
                for (result in results) {
                    writer.write(
                        CsvQuoting.writeCsvRow(
                            listOf(result.className, result.methodName, result.outcome.toString()),
                        ),
                    )
                }
            }
        } catch (e: Exception) {
            System.err.println("Error writing CSV to ${outputFile.path}: ${e.message}")
            exitProcess(3)
        }
    }
}
