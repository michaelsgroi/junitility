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
        assert(value != 20 && value != 50) { "Values 20 and 50 should fail" }
    }

    @ParameterizedTest
    @ValueSource(ints = [100, 200, 300, 400, 500])
    fun testWithErrors(value: Int) {
        if (value == 200 || value == 400) {
            throw RuntimeException("Intentional error for value $value")
        }
        assert(value > 0)
    }

    @ParameterizedTest
    @ValueSource(strings = ["alpha", "beta", "gamma", "delta"])
    fun testOnlyInTestOne(str: String) {
        assert(str.length > 3)
    }
}
