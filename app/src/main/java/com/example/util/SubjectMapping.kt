package com.example.util

object SubjectMapping {
    val OFFICIAL_SUBJECT_CODES = listOf(
        "CS030601",
        "CS030602",
        "CS030603",
        "CS030604",
        "CS030605",
        "HS030601"
    )

    val LEGACY_SUBJECT_CODES = setOf("CS101", "HS101", "HS505")

    val SUBJECT_MAP = mapOf(
        "CS030601" to "Data Structures",
        "CS030602" to "Object Oriented Programming",
        "CS030603" to "Database Management Systems",
        "CS030604" to "Discrete Mathematics and Graph Theory",
        "CS030605" to "Operating Systems",
        "HS030601" to "Social and Professional Ethics"
    )

    fun getSubjectTitle(courseCode: String): String {
        val cleanCode = courseCode.trim().uppercase()
        return SUBJECT_MAP[cleanCode] ?: cleanCode
    }

    fun isLegacyCode(courseCode: String): Boolean {
        return courseCode.trim().uppercase() in LEGACY_SUBJECT_CODES
    }
}
