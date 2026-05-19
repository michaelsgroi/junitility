package com.michaelsgroi.test.junitility.integration

import org.junit.jupiter.api.Test

class MixedResultsTest {
    @Test
    fun testSuccess1() {
        assert(true)
    }

    @Test
    fun testSuccess2() {
        assert(true)
    }

    @Test
    fun testSuccess3() {
        assert(true)
    }

    @Test
    fun testFailure1() {
        assert(false) { "Different failure in test-two" }
    }

    @Test
    fun testError1() {
        throw RuntimeException("Different error in test-two")
    }

    @Test
    fun testSuccess4() {
        assert(true)
    }

    @Test
    fun testSuccess5() {
        assert(true)
    }

    @Test
    fun testUniqueToTwo() {
        assert(false) { "Unique test in test-two" }
    }
}
