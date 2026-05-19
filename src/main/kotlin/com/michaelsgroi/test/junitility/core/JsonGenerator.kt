package com.michaelsgroi.test.junitility.core

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.michaelsgroi.test.junitility.model.ClassSummary
import com.michaelsgroi.test.junitility.model.MethodSummary
import com.michaelsgroi.test.junitility.model.Outcome
import com.michaelsgroi.test.junitility.model.TestResult
import com.michaelsgroi.test.junitility.model.TestSummary
import java.io.File
import kotlin.system.exitProcess

object JsonGenerator {
    private val objectMapper =
        jacksonObjectMapper().apply {
            enable(SerializationFeature.INDENT_OUTPUT)
        }

    fun generate(
        results: List<TestResult>,
        outputFile: File,
        csvPath: String,
    ) {
        try {
            val summary = aggregateResults(results, csvPath)
            outputFile.parentFile?.mkdirs()
            objectMapper.writeValue(outputFile, summary)
        } catch (e: Exception) {
            System.err.println("Error writing JSON to ${outputFile.path}: ${e.message}")
            exitProcess(3)
        }
    }

    private fun aggregateResults(
        results: List<TestResult>,
        csvPath: String,
    ): TestSummary {
        val classSummaries = mutableListOf<ClassSummary>()

        val byClass = results.groupBy { it.className }
        for ((className, classResults) in byClass) {
            val methodSummaries = mutableListOf<MethodSummary>()

            val byMethod = classResults.groupBy { it.methodName }
            for ((methodName, methodResults) in byMethod) {
                methodSummaries.add(
                    MethodSummary(
                        methodName = methodName,
                        total = methodResults.size,
                        success = methodResults.count { it.outcome == Outcome.SUCCESS },
                        failures = methodResults.count { it.outcome == Outcome.FAILURE },
                        errors = methodResults.count { it.outcome == Outcome.ERROR },
                        skipped = methodResults.count { it.outcome == Outcome.SKIPPED },
                    ),
                )
            }

            classSummaries.add(
                ClassSummary(
                    className = className,
                    total = classResults.size,
                    success = classResults.count { it.outcome == Outcome.SUCCESS },
                    failures = classResults.count { it.outcome == Outcome.FAILURE },
                    errors = classResults.count { it.outcome == Outcome.ERROR },
                    skipped = classResults.count { it.outcome == Outcome.SKIPPED },
                    methods = methodSummaries,
                ),
            )
        }

        return TestSummary(
            detailedReportPath = csvPath,
            classes = classSummaries,
        )
    }
}
