package com.michaelsgroi.test.junitility.model

enum class NetChange {
    ADDED,
    REMOVED,
    FIXED,
    SKIPPED_FIXED,
    REGRESSED,
    FAILURE_ERROR,
    ERROR_FAILURE,
    SKIPPED,
    FAILURE_SKIPPED,
    ERROR_SKIPPED,
    SKIPPED_FAILURE,
    SKIPPED_ERROR,
    SUCCESS,
    FAILURE,
    ERROR,
    SKIPPED_REMAINED,
    ;

    fun toCsvString(): String =
        when (this) {
            ADDED -> "ADDED"
            REMOVED -> "REMOVED"
            FIXED -> "FIXED"
            SKIPPED_FIXED -> "SKIPPED-FIXED"
            REGRESSED -> "REGRESSED"
            FAILURE_ERROR -> "FAILURE-ERROR"
            ERROR_FAILURE -> "ERROR-FAILURE"
            SKIPPED -> "SKIPPED"
            FAILURE_SKIPPED -> "FAILURE-SKIPPED"
            ERROR_SKIPPED -> "ERROR-SKIPPED"
            SKIPPED_FAILURE -> "SKIPPED-FAILURE"
            SKIPPED_ERROR -> "SKIPPED-ERROR"
            SUCCESS -> "SUCCESS"
            FAILURE -> "FAILURE"
            ERROR -> "ERROR"
            SKIPPED_REMAINED -> "SKIPPED"
        }
}
