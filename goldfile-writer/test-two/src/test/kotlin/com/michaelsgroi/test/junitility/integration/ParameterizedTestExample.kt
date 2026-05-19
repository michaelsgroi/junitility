package com.michaelsgroi.test.junitility.integration

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ParameterizedTestExample {
    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3, 4, 5])
    fun testAllPass(value: Int) {
        assert(value > 0)
    }

    @ParameterizedTest
    @ValueSource(strings = ["hello", "world", "test"])
    fun testStringsAllPass(str: String) {
        assert(str.isNotEmpty())
    }

    @ParameterizedTest
    @ValueSource(ints = [10, 20, 30, 40, 50, 60, 70, 80])
    fun testWithMixedResults(value: Int) {
        assert(value != 30 && value != 60) { "Values 30 and 60 should fail in test-two" }
    }

    @ParameterizedTest
    @ValueSource(ints = [100, 200, 300, 400, 500])
    fun testWithErrors(value: Int) {
        if (value == 300 || value == 500) {
            throw RuntimeException("Different errors in test-two for value $value")
        }
        assert(value > 0)
    }
}
