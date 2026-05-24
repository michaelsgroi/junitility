package com.michaelsgroi.test.junitility.core

object CsvQuoting {
    fun quoteField(field: String): String {
        if (field.contains(',') || field.contains('"') || field.contains('\n')) {
            return "\"${field.replace("\"", "\"\"")}\""
        }
        return field
    }

    fun writeCsvRow(fields: List<String>): String = fields.joinToString(",") { quoteField(it) } + "\n"
}
