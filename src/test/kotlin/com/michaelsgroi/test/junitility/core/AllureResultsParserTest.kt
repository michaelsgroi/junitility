package com.michaelsgroi.test.junitility.core

import com.michaelsgroi.test.junitility.model.NetChange
import com.michaelsgroi.test.junitility.model.Outcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class AllureResultsParserTest {
    @Test
    fun `preserves nested class and parameterized test identities`() {
        val results = AllureResultsParser.parseDirectory(fixtureDirectory("baseline"))

        assertEquals(4, results.size)
        assertTrue(
            results.any {
                it.className == "com.example.item.GetItemConformanceTest\$Validation" &&
                    it.methodName == "rejectsWrongShapeTenantId(TenantIdShapeCase) rejects missing active type" &&
                    it.outcome == Outcome.FAILURE
            },
        )
        assertTrue(
            results.any {
                it.className == "com.example.item.PutItemConformanceTest\$Validation" &&
                    it.methodName == "rejectsWrongShapeTenantId(TenantIdShapeCase) rejects missing active type" &&
                    it.outcome == Outcome.ERROR
            },
        )
        assertTrue(
            results.any {
                it.className == "com.example.TenantTest" &&
                    it.methodName == "rejectsTenantId(TenantIdShapeCase) rejects missing active type" &&
                    it.outcome == Outcome.ERROR
            },
        )
        assertTrue(
            results.any {
                it.className == "com.example.TenantTest" &&
                    it.methodName == "rejectsTenantId(TenantIdShapeCase) rejects invalid shape" &&
                    it.outcome == Outcome.SUCCESS
            },
        )
    }

    @Test
    fun `compares nested classes and parameterized variants independently`() {
        val impact =
            NetImpactGenerator.compare(
                AllureResultsParser.parseDirectory(fixtureDirectory("baseline")),
                AllureResultsParser.parseDirectory(fixtureDirectory("patched")),
            )

        assertEquals(5, impact.testResults.size)
        assertEquals(3, impact.testResults.count { it.netChange == NetChange.FIXED })
        assertEquals(1, impact.testResults.count { it.netChange == NetChange.REGRESSED })
        assertEquals(1, impact.testResults.count { it.netChange == NetChange.ADDED })
        assertTrue(
            impact.testResults.any {
                it.className == "com.example.item.GetItemConformanceTest\$Validation" && it.netChange == NetChange.FIXED
            },
        )
        assertTrue(
            impact.testResults.any {
                it.className == "com.example.item.PutItemConformanceTest\$Validation" && it.netChange == NetChange.FIXED
            },
        )
    }

    @Test
    fun `distinguishes duplicate Allure display names by template invocation`() {
        val results = AllureResultsParser.parseDirectory(fixtureDirectory("duplicate-display-name"))

        assertEquals(2, results.size)
        assertEquals(
            setOf("rejects(FilterCase) FortyTwo BETWEEN :lo AND :hi"),
            results.map { it.methodName }.toSet(),
        )
        assertEquals(2, results.mapNotNull { it.comparisonIdentity }.toSet().size)
        assertTrue(results.any { it.comparisonIdentity!!.endsWith("[test-template-invocation:#1]") })
        assertTrue(results.any { it.comparisonIdentity!!.endsWith("[test-template-invocation:#5]") })

        val baseline = results.mapIndexed { index, result -> result.copy(outcome = if (index == 0) Outcome.FAILURE else Outcome.SUCCESS) }
        val patched = results.mapIndexed { index, result -> result.copy(outcome = if (index == 0) Outcome.SUCCESS else Outcome.FAILURE) }
        val impact = NetImpactGenerator.compare(baseline, patched)

        assertEquals(2, impact.testResults.size)
        assertEquals(1, impact.testResults.count { it.netChange == NetChange.FIXED })
        assertEquals(1, impact.testResults.count { it.netChange == NetChange.REGRESSED })
    }

    @Test
    fun `compares unstable Allure display names by JUnit Platform identity`() {
        val baseline = AllureResultsParser.parseDirectory(fixtureDirectory("unstable-display-name/baseline"))
        val patched = AllureResultsParser.parseDirectory(fixtureDirectory("unstable-display-name/patched"))
        val impact = NetImpactGenerator.compare(baseline, patched)

        assertEquals(1, impact.testResults.size)
        assertEquals(NetChange.SUCCESS, impact.testResults.single().netChange)
        assertEquals("rejectKeyAttributeTypeMismatch(KeyTypeMismatchCase) Binary value [B@baseline", impact.testResults.single().methodName)
    }

    private fun fixtureDirectory(name: String): File = File("src/test/resources/allure-gold-data/$name")
}
