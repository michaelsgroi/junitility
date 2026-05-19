package com.michaelsgroi.test.junitility.integration

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class BeforeAllFailureTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setupThatFails(): Unit = throw RuntimeException("BeforeAll failed")
    }

    @Test
    fun testNeverRuns1() {
        assert(true)
    }

    @Test
    fun testNeverRuns2() {
        assert(true)
    }
}
