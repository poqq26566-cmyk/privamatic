package com.techtrest.privamatic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "privacy_snapshots")
data class PrivacySnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val score: Int,
    // Per-check deductions as JSON: [{"check":"USB_DEBUGGING","points":4},...]
    val deductionsJson: String
)

data class CheckDeduction(
    val checkName: String,
    val points: Int
) {
    companion object {
        fun encode(deductions: List<CheckDeduction>): String {
            if (deductions.isEmpty()) return "[]"
            return deductions.joinToString(",", "[", "]") { d ->
                "{\"check\":\"${d.checkName}\",\"points\":${d.points}}"
            }
        }

        fun decode(json: String): List<CheckDeduction> {
            val trimmed = json.trim()
            if (trimmed == "[]" || trimmed.isEmpty()) return emptyList()
            val inner = trimmed.removePrefix("[").removeSuffix("]").trim()
            if (inner.isEmpty()) return emptyList()
            return inner.split("},{").map { part ->
                val clean = part.removePrefix("{").removeSuffix("}")
                CheckDeduction(
                    checkName = extractStringValue(clean, "check"),
                    points = extractIntValue(clean, "points")
                )
            }
        }

        private fun extractStringValue(json: String, key: String): String {
            val pattern = "\"$key\":\""
            val start = json.indexOf(pattern)
            if (start < 0) return ""
            val valueStart = start + pattern.length
            val valueEnd = json.indexOf("\"", valueStart)
            if (valueEnd < 0) return ""
            return json.substring(valueStart, valueEnd)
        }

        private fun extractIntValue(json: String, key: String): Int {
            val pattern = "\"$key\":"
            val start = json.indexOf(pattern)
            if (start < 0) return 0
            val valueStart = start + pattern.length
            val commaIdx = json.indexOf(",", valueStart)
            val braceIdx = json.indexOf("}", valueStart)
            val valueEnd = when {
                commaIdx < 0 && braceIdx < 0 -> json.length
                commaIdx < 0 -> braceIdx
                braceIdx < 0 -> commaIdx
                else -> minOf(commaIdx, braceIdx)
            }
            return json.substring(valueStart, valueEnd).trim().toIntOrNull() ?: 0
        }
    }
}
