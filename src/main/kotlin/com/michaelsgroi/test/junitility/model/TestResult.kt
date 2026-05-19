package com.michaelsgroi.test.junitility.model

data class TestResult(
    val className: String,
    val methodName: String,
    val outcome: Outcome,
)
