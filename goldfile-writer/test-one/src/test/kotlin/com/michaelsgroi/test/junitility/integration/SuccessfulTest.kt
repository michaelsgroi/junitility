package com.michaelsgroi.test.junitility.integration

import org.junit.jupiter.api.Test

class SuccessfulTest {
    @Test
    fun testSuccess1() {
        assert(true)
    }

    @Test
    fun testSuccess2() {
        assert(1 + 1 == 2)
    }

    @Test
    fun testThatPassesHere() {
        assert(true)
    }
}
