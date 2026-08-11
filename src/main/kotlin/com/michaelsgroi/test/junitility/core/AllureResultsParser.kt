package com.michaelsgroi.test.junitility.core

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.michaelsgroi.test.junitility.model.Outcome
import com.michaelsgroi.test.junitility.model.TestResult
import java.io.File
import kotlin.system.exitProcess

object AllureResultsParser {
    private val objectMapper = jacksonObjectMapper()

    fun parseDirectory(
        dir: File,
        debug: Boolean = false,
        debugLog: (String) -> Unit = {},
    ): List<TestResult> {
        val resultFiles =
            dir
                .walkTopDown()
                .filter { it.isFile && it.name.endsWith("-result.json") }
                .sortedBy { it.path }
                .toList()
        if (resultFiles.isEmpty()) {
            System.err.println("Error: No *-result.json files found in ${dir.path}")
            exitProcess(2)
        }

        if (debug) debugLog("Found ${resultFiles.size} *-result.json files")

        val results = mutableListOf<ParsedResult>()
        try {
            resultFiles.forEach { file -> results.add(parseResult(file, debug, debugLog)) }
            val testResults = assignComparisonIdentities(results)
            if (debug) debugLog("Parsed ${testResults.size} test results")
            return testResults
        } catch (e: AllureParseException) {
            System.err.println(e.message)
            exitProcess(4)
        }
    }

    private fun parseResult(
        file: File,
        debug: Boolean,
        debugLog: (String) -> Unit,
    ): ParsedResult {
        try {
            if (debug) debugLog("Parsing ${file.name}")
            val node = objectMapper.readTree(file)
            val fullName = requiredText(node.path("fullName"), "fullName", file)
            val name = requiredText(node.path("name"), "name", file)
            val status = requiredText(node.path("status"), "status", file)
            val separatorIndex = fullName.lastIndexOf('.')
            if (separatorIndex <= 0 || separatorIndex == fullName.lastIndex) {
                parseError(file, "fullName must include a class name and method name")
            }

            val className = fullName.substring(0, separatorIndex)
            return ParsedResult(className, fullName, name, junitPlatformUniqueId(node), mapOutcome(status, file), file)
        } catch (e: AllureParseException) {
            throw e
        } catch (e: Exception) {
            parseError(file, e.message ?: "invalid JSON")
        }
    }

    private fun assignComparisonIdentities(results: List<ParsedResult>): List<TestResult> {
        val identities = mutableMapOf<Pair<String, String>, ParsedResult>()

        return results.map { result ->
            val comparisonIdentity = result.uniqueId ?: result.name
            val identity = result.className to comparisonIdentity
            val previous = identities.putIfAbsent(identity, result)
            if (previous != null) {
                parseError(
                    result.file,
                    "Duplicate Allure test identity '${result.fullName}' and '$comparisonIdentity' also found in ${previous.file.path}",
                )
            }
            TestResult(result.className, result.name, result.outcome, result.uniqueId)
        }
    }

    private fun junitPlatformUniqueId(node: JsonNode): String? =
        node
            .path("labels")
            .filter { it.path("name").asText() == "junit.platform.uniqueid" }
            .firstOrNull()
            ?.path("value")
            ?.asText()
            ?.takeIf { it.isNotBlank() }

    private fun requiredText(
        node: JsonNode,
        field: String,
        file: File,
    ): String {
        if (!node.isTextual || node.asText().isBlank()) {
            parseError(file, "missing or blank $field")
        }
        return node.asText()
    }

    private fun mapOutcome(
        status: String,
        file: File,
    ): Outcome =
        when (status) {
            "passed" -> Outcome.SUCCESS
            "failed" -> Outcome.FAILURE
            "broken" -> Outcome.ERROR
            "skipped" -> Outcome.SKIPPED
            else -> parseError(file, "unknown status '$status'")
        }

    private fun parseError(
        file: File,
        message: String,
    ): Nothing = throw AllureParseException("Error parsing ${file.path}: $message")

    private class AllureParseException(
        message: String,
    ) : RuntimeException(message)

    private data class ParsedResult(
        val className: String,
        val fullName: String,
        val name: String,
        val uniqueId: String?,
        val outcome: Outcome,
        val file: File,
    )
}
