package com.michaelsgroi.test.junitility.integration

import org.junit.jupiter.api.Test

class FailingTest {
    @Test
    fun testThatFails() {
        assert(false) { "This test is expected to fail" }
    }

    @Test
    fun testThatThrowsError(): Unit = throw RuntimeException("Intentional error")

    @Test
    fun testThatSucceeds() {
        assert(true)
    }
}
