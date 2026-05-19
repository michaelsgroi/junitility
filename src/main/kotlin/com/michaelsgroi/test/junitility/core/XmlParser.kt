package com.michaelsgroi.test.junitility.core

import com.michaelsgroi.test.junitility.model.Outcome
import com.michaelsgroi.test.junitility.model.TestResult
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger
import org.apache.maven.plugins.surefire.report.ReportTestCase
import org.apache.maven.plugins.surefire.report.TestSuiteXmlParser
import java.io.File
import kotlin.system.exitProcess

object XmlParser {
    private val consoleLogger =
        object : ConsoleLogger {
            override fun debug(message: String?) {}

            override fun info(message: String?) {}

            override fun warning(message: String?) {}

            override fun error(message: String?) {}

            override fun error(
                message: String?,
                throwable: Throwable?,
            ) {}

            override fun error(throwable: Throwable?) {}

            override fun isDebugEnabled(): Boolean = false

            override fun isInfoEnabled(): Boolean = false

            override fun isWarnEnabled(): Boolean = false

            override fun isErrorEnabled(): Boolean = false
        }

    fun parseDirectory(
        dir: File,
        debug: Boolean = false,
        debugLog: (String) -> Unit = {},
    ): List<TestResult> {
        if (!dir.exists() || !dir.isDirectory) {
            System.err.println("Error: ${dir.path} is not a valid directory")
            exitProcess(2)
        }

        val xmlFiles = findTestXmlFiles(dir)
        if (xmlFiles.isEmpty()) {
            System.err.println("Error: No TEST-*.xml files found in ${dir.path}")
            exitProcess(2)
        }

        if (debug) debugLog("Found ${xmlFiles.size} TEST-*.xml files")

        val results = mutableListOf<TestResult>()
        val parser = TestSuiteXmlParser(consoleLogger)

        for (xmlFile in xmlFiles) {
            try {
                if (debug) debugLog("Parsing ${xmlFile.name}")
                val suites = parser.parse(xmlFile.absolutePath)
                for (suite in suites) {
                    for (testCase in suite.testCases) {
                        results.add(convertToTestResult(testCase))
                    }
                }
            } catch (e: Exception) {
                System.err.println("Error parsing ${xmlFile.path}: ${e.message}")
                exitProcess(4)
            }
        }

        if (debug) debugLog("Parsed ${results.size} test results")

        return results
    }

    private fun findTestXmlFiles(dir: File): List<File> {
        val xmlFiles = mutableListOf<File>()
        dir.walkTopDown().forEach { file ->
            if (file.isFile && file.name.startsWith("TEST-") && file.name.endsWith(".xml")) {
                xmlFiles.add(file)
            }
        }
        return xmlFiles
    }

    private fun convertToTestResult(testCase: ReportTestCase): TestResult {
        val outcome =
            when {
                testCase.isSuccessful -> Outcome.SUCCESS
                testCase.hasFailure() -> Outcome.FAILURE
                testCase.hasError() -> Outcome.ERROR
                testCase.hasSkipped() -> Outcome.SKIPPED
                else -> Outcome.ERROR
            }

        return TestResult(
            className = testCase.fullClassName,
            methodName = testCase.name,
            outcome = outcome,
        )
    }
}
