package com.michaelsgroi.test.junitility.integration

import org.junit.jupiter.api.Test

class SuccessfulTest {
    @Test
    fun testSuccess1() {
        assert(true)
    }

    @Test
    fun testSuccess2() {
        assert(true)
    }

    @Test
    fun testThatPassesHere() {
        assert(false) { "This test passes in test-one but fails in test-two" }
    }
}
