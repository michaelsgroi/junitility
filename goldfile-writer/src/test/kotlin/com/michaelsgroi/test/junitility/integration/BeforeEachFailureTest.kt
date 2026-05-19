package com.michaelsgroi.test.junitility.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BeforeEachFailureTest {
    @BeforeEach
    fun setupThatFails(): Unit = throw RuntimeException("BeforeEach failed")

    @Test
    fun testWithFailingSetup1() {
        assert(true)
    }

    @Test
    fun testWithFailingSetup2() {
        assert(true)
    }
}
