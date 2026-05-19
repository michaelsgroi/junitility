package com.michaelsgroi.test.junitility.core

import java.io.File
import kotlin.system.exitProcess

data class TestKey(
    val className: String,
    val methodName: String,
)

object CsvDiffer {
    fun generateDiff(
        csv1: File,
        csv2: File,
        outputFile: File,
        dir1Name: String,
        dir2Name: String,
    ) {
        val results1 = parseCsv(csv1)
        val results2 = parseCsv(csv2)

        val allKeys = (results1.keys + results2.keys).sortedWith(compareBy({ it.className }, { it.methodName }))

        outputFile.bufferedWriter().use { writer ->
            writer.write("ClassName,MethodName,${quoteCsvField(dir1Name)} Outcome,${quoteCsvField(dir2Name)} Outcome\n")

            for (key in allKeys) {
                val outcome1 = results1[key] ?: "-"
                val outcome2 = results2[key] ?: "-"

                writer.write(
                    "${quoteCsvField(key.className)},${quoteCsvField(key.methodName)}," +
                        "${quoteCsvField(outcome1)},${quoteCsvField(outcome2)}\n",
                )
            }
        }
    }

    private fun parseCsv(csv: File): Map<TestKey, String> {
        val results = mutableMapOf<TestKey, String>()

        try {
            csv.bufferedReader().use { reader ->
                val header = reader.readLine()
                if (header == null || !header.startsWith("ClassName,MethodName,Outcome")) {
                    System.err.println("Error: Invalid CSV format in ${csv.path}")
                    exitProcess(4)
                }

                reader.lineSequence().forEach { line ->
                    val parts = parseCsvLine(line)
                    if (parts.size >= 3) {
                        val key = TestKey(parts[0], parts[1])
                        results[key] = parts[2]
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("Error reading ${csv.path}: ${e.message}")
            exitProcess(3)
        }

        return results
    }

    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        var currentField = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val ch = line[i]

            when {
                ch == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    currentField.append('"')
                    i++
                }

                ch == '"' -> {
                    inQuotes = !inQuotes
                }

                ch == ',' && !inQuotes -> {
                    fields.add(currentField.toString())
                    currentField = StringBuilder()
                }

                else -> {
                    currentField.append(ch)
                }
            }
            i++
        }

        fields.add(currentField.toString())
        return fields
    }

    private fun quoteCsvField(field: String): String {
        if (field.contains(',') || field.contains('"') || field.contains('\n')) {
            return "\"${field.replace("\"", "\"\"")}\""
        }
        return field
    }
}
