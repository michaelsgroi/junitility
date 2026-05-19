package com.michaelsgroi.test.junitility.model

data class TestSummary(
    val detailedReportPath: String,
    val classes: List<ClassSummary>,
)

data class ClassSummary(
    val className: String,
    val total: Int,
    val success: Int,
    val failures: Int,
    val errors: Int,
    val skipped: Int,
    val methods: List<MethodSummary>,
)

data class MethodSummary(
    val methodName: String,
    val total: Int,
    val success: Int,
    val failures: Int,
    val errors: Int,
    val skipped: Int,
)
