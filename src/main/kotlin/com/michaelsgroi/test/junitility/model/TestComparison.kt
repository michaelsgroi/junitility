package com.michaelsgroi.test.junitility.model

data class TestComparison(
    val className: String,
    val methodName: String,
    val baselineOutcome: Outcome?,
    val patchedOutcome: Outcome?,
    val netChange: NetChange,
)
