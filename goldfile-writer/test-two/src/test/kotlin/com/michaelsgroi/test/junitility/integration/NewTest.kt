package com.michaelsgroi.test.junitility.integration

import org.junit.jupiter.api.Test

class NewTest {
    @Test
    fun testUnique() {
        assert(true)
    }

    @Test
    fun testAnother() {
        assert(false) { "This test fails" }
    }
}
