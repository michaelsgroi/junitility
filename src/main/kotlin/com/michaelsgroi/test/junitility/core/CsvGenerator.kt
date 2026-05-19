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
                writer.write("ClassName,MethodName,Outcome\n")
                for (result in results) {
                    writer.write(
                        "${quoteCsvField(result.className)},${quoteCsvField(result.methodName)},${result.outcome}\n",
                    )
                }
            }
        } catch (e: Exception) {
            System.err.println("Error writing CSV to ${outputFile.path}: ${e.message}")
            exitProcess(3)
        }
    }

    private fun quoteCsvField(field: String): String {
        if (field.contains(',') || field.contains('"') || field.contains('\n')) {
            return "\"${field.replace("\"", "\"\"")}\""
        }
        return field
    }
}
