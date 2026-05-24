package com.michaelsgroi.test.junitility.core

import com.michaelsgroi.test.junitility.model.NetChange
import com.michaelsgroi.test.junitility.model.NetImpact
import com.michaelsgroi.test.junitility.model.Outcome
import com.michaelsgroi.test.junitility.model.TestComparison
import com.michaelsgroi.test.junitility.model.TestKey
import com.michaelsgroi.test.junitility.model.TestResult
import java.io.File
import kotlin.system.exitProcess

object NetImpactGenerator {
    fun compare(
        baseline: List<TestResult>,
        patched: List<TestResult>,
    ): NetImpact {
        val baselineMap = baseline.associate { TestKey(it.className, it.methodName) to it.outcome }
        val patchedMap = patched.associate { TestKey(it.className, it.methodName) to it.outcome }

        val allKeys = (baselineMap.keys + patchedMap.keys).toSet()

        val comparisons =
            allKeys.map { key ->
                val baselineOutcome = baselineMap[key]
                val patchedOutcome = patchedMap[key]
                val netChange = determineNetChange(baselineOutcome, patchedOutcome)

                TestComparison(
                    className = key.className,
                    methodName = key.methodName,
                    baselineOutcome = baselineOutcome,
                    patchedOutcome = patchedOutcome,
                    netChange = netChange,
                )
            }

        return NetImpact(comparisons)
    }

    fun generateSummaryMarkdown(
        impact: NetImpact,
        outputFile: File,
    ) {
        try {
            outputFile.parentFile?.mkdirs()

            val baselineTotals = calculateOutcomeCounts(impact.testResults.mapNotNull { it.baselineOutcome })
            val patchedTotals = calculateOutcomeCounts(impact.testResults.mapNotNull { it.patchedOutcome })

            val baselineTotal = impact.testResults.count { it.baselineOutcome != null }
            val patchedTotal = impact.testResults.count { it.patchedOutcome != null }

            val fixedTests = impact.testResults.count { it.netChange == NetChange.FIXED }
            val unskippedFixedTests = impact.testResults.count { it.netChange == NetChange.SKIPPED_FIXED }
            val addedTests = impact.testResults.count { it.netChange == NetChange.ADDED }
            val newlyUnskippedTests =
                impact.testResults.count {
                    it.baselineOutcome == Outcome.SKIPPED && it.patchedOutcome != null && it.patchedOutcome != Outcome.SKIPPED
                }
            val regressedTests = impact.testResults.count { it.netChange == NetChange.REGRESSED }
            val newlySkippedTests =
                impact.testResults.count {
                    it.baselineOutcome != null && it.baselineOutcome != Outcome.SKIPPED && it.patchedOutcome == Outcome.SKIPPED
                }
            val removedTests = impact.testResults.count { it.netChange == NetChange.REMOVED }

            val markdown =
                buildString {
                    appendLine("# PR Impact Summary")
                    appendLine()
                    appendLine("**Total Tests:**  ")
                    appendLine("Baseline: $baselineTotal | Patched: $patchedTotal | Delta: ${formatDelta(patchedTotal - baselineTotal)}")
                    appendLine()
                    appendLine("**Success:**  ")
                    appendLine(
                        "Baseline: ${baselineTotals.success} | Patched: ${patchedTotals.success} | Delta: ${formatDelta(
                            patchedTotals.success - baselineTotals.success,
                        )}",
                    )
                    appendLine()
                    appendLine("**Failures:**  ")
                    appendLine(
                        "Baseline: ${baselineTotals.failures} | Patched: ${patchedTotals.failures} | Delta: ${formatDelta(
                            patchedTotals.failures - baselineTotals.failures,
                        )}",
                    )
                    appendLine()
                    appendLine("**Errors:**  ")
                    appendLine(
                        "Baseline: ${baselineTotals.errors} | Patched: ${patchedTotals.errors} | Delta: ${formatDelta(
                            patchedTotals.errors - baselineTotals.errors,
                        )}",
                    )
                    appendLine()
                    appendLine("**Skipped:**  ")
                    appendLine(
                        "Baseline: ${baselineTotals.skipped} | Patched: ${patchedTotals.skipped} | Delta: ${formatDelta(
                            patchedTotals.skipped - baselineTotals.skipped,
                        )}",
                    )
                    appendLine()
                    appendLine("**Fixed Tests:** $fixedTests  ")
                    appendLine("**Unskipped-Fixed Tests:** $unskippedFixedTests  ")
                    appendLine("**Added Tests:** $addedTests  ")
                    appendLine("**Newly Unskipped Tests:** $newlyUnskippedTests  ")
                    appendLine("**Regressed Tests:** $regressedTests  ")
                    appendLine("**Newly Skipped Tests:** $newlySkippedTests  ")
                    append("**Removed Tests:** $removedTests")
                }

            outputFile.writeText(markdown)
        } catch (e: Exception) {
            System.err.println("Error writing summary markdown to ${outputFile.path}: ${e.message}")
            exitProcess(3)
        }
    }

    fun generateDetailedCsv(
        impact: NetImpact,
        outputFile: File,
    ) {
        try {
            outputFile.parentFile?.mkdirs()

            val sortedComparisons =
                impact.testResults.sortedWith(
                    compareBy<TestComparison, String>(String.CASE_INSENSITIVE_ORDER) { it.className }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.methodName },
                )

            outputFile.bufferedWriter().use { writer ->
                writer.write(
                    CsvQuoting.writeCsvRow(
                        listOf("ClassName", "MethodName", "BaselineOutcome", "PatchedOutcome", "NetChange"),
                    ),
                )

                for (comparison in sortedComparisons) {
                    val baselineOutcomeStr = comparison.baselineOutcome?.toString() ?: "-"
                    val patchedOutcomeStr = comparison.patchedOutcome?.toString() ?: "-"
                    val netChangeStr = comparison.netChange.toCsvString()

                    writer.write(
                        CsvQuoting.writeCsvRow(
                            listOf(
                                comparison.className,
                                comparison.methodName,
                                baselineOutcomeStr,
                                patchedOutcomeStr,
                                netChangeStr,
                            ),
                        ),
                    )
                }
            }
        } catch (e: Exception) {
            System.err.println("Error writing detailed CSV to ${outputFile.path}: ${e.message}")
            exitProcess(3)
        }
    }

    private fun formatDelta(value: Int): String =
        when {
            value > 0 -> "+$value"
            value < 0 -> value.toString()
            else -> "0"
        }

    private fun determineNetChange(
        baseline: Outcome?,
        patched: Outcome?,
    ): NetChange =
        when {
            baseline == null && patched != null -> {
                NetChange.ADDED
            }

            baseline != null && patched == null -> {
                NetChange.REMOVED
            }

            baseline != null && patched != null -> {
                when (baseline) {
                    Outcome.SUCCESS -> {
                        when (patched) {
                            Outcome.SUCCESS -> NetChange.SUCCESS
                            Outcome.FAILURE, Outcome.ERROR -> NetChange.REGRESSED
                            Outcome.SKIPPED -> NetChange.SKIPPED
                        }
                    }

                    Outcome.FAILURE -> {
                        when (patched) {
                            Outcome.SUCCESS -> NetChange.FIXED
                            Outcome.FAILURE -> NetChange.FAILURE
                            Outcome.ERROR -> NetChange.FAILURE_ERROR
                            Outcome.SKIPPED -> NetChange.FAILURE_SKIPPED
                        }
                    }

                    Outcome.ERROR -> {
                        when (patched) {
                            Outcome.SUCCESS -> NetChange.FIXED
                            Outcome.FAILURE -> NetChange.ERROR_FAILURE
                            Outcome.ERROR -> NetChange.ERROR
                            Outcome.SKIPPED -> NetChange.ERROR_SKIPPED
                        }
                    }

                    Outcome.SKIPPED -> {
                        when (patched) {
                            Outcome.SUCCESS -> NetChange.SKIPPED_FIXED
                            Outcome.FAILURE -> NetChange.SKIPPED_FAILURE
                            Outcome.ERROR -> NetChange.SKIPPED_ERROR
                            Outcome.SKIPPED -> NetChange.SKIPPED_REMAINED
                        }
                    }
                }
            }

            else -> {
                throw IllegalStateException("Both outcomes null")
            }
        }

    private data class OutcomeCounts(
        val success: Int,
        val failures: Int,
        val errors: Int,
        val skipped: Int,
    )

    private fun calculateOutcomeCounts(outcomes: List<Outcome>): OutcomeCounts =
        OutcomeCounts(
            success = outcomes.count { it == Outcome.SUCCESS },
            failures = outcomes.count { it == Outcome.FAILURE },
            errors = outcomes.count { it == Outcome.ERROR },
            skipped = outcomes.count { it == Outcome.SKIPPED },
        )
}
