package com.michaelsgroi.test.junitility.integration

import org.junit.jupiter.api.Test

class MixedResultsTest {
    @Test
    fun testSuccess1() {
        assert(true)
    }

    @Test
    fun testSuccess2() {
        assert(1 + 1 == 2)
    }

    @Test
    fun testFailure1() {
        assert(false) { "Expected failure 1" }
    }

    @Test
    fun testSuccess3() {
        assert("hello".length == 5)
    }

    @Test
    fun testError1(): Unit = throw IllegalStateException("Expected error 1")

    @Test
    fun testSuccess4() {
        assert(listOf(1, 2, 3).size == 3)
    }

    @Test
    fun testFailure2() {
        assert(false) { "Expected failure 2" }
    }

    @Test
    fun testSuccess5() {
        assert(true)
    }

    @Test
    fun testError2(): Unit = throw NullPointerException("Expected error 2")
}
