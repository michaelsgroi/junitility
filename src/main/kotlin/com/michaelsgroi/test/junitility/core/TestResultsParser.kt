package com.michaelsgroi.test.junitility.core

import com.michaelsgroi.test.junitility.model.TestResult
import java.io.File
import kotlin.system.exitProcess

object TestResultsParser {
    fun parseDirectory(
        dir: File,
        format: InputFormat = InputFormat.AUTO,
        debug: Boolean = false,
        debugLog: (String) -> Unit = {},
    ): List<TestResult> =
        when (detectFormat(dir, format)) {
            InputFormat.SUREFIRE -> XmlParser.parseDirectory(dir, debug, debugLog)
            InputFormat.ALLURE -> AllureResultsParser.parseDirectory(dir, debug, debugLog)
            InputFormat.AUTO -> throw IllegalStateException("AUTO must be resolved before parsing")
        }

    private fun detectFormat(
        dir: File,
        requestedFormat: InputFormat,
    ): InputFormat {
        if (!dir.exists() || !dir.isDirectory) {
            System.err.println("Error: ${dir.path} is not a valid directory")
            exitProcess(2)
        }

        val hasAllureResults = dir.walkTopDown().any { it.isFile && it.name.endsWith("-result.json") }
        val hasSurefireResults = dir.walkTopDown().any { it.isFile && it.name.startsWith("TEST-") && it.name.endsWith(".xml") }

        return when (requestedFormat) {
            InputFormat.AUTO -> {
                when {
                    hasAllureResults -> InputFormat.ALLURE
                    hasSurefireResults -> InputFormat.SUREFIRE
                    else -> noResultsFound(dir, "TEST-*.xml or *-result.json")
                }
            }

            InputFormat.SUREFIRE -> {
                if (hasSurefireResults) InputFormat.SUREFIRE else noResultsFound(dir, "TEST-*.xml")
            }

            InputFormat.ALLURE -> {
                if (hasAllureResults) InputFormat.ALLURE else noResultsFound(dir, "*-result.json")
            }
        }
    }

    private fun noResultsFound(
        dir: File,
        expectedFormat: String,
    ): Nothing {
        System.err.println("Error: No $expectedFormat files found in ${dir.path}")
        exitProcess(2)
    }
}
