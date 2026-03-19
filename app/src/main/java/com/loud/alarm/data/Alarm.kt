package com.loud.alarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

enum class ChallengeType {
    NONE, MATH, QR_CODE, REWRITE, STEP, MAZE, MEMORY, SHAKE, TYPING, PUZZLE, SCAN_SINK, SCAN_OBJECT
}

enum class MathDifficulty {
    EASY, MEDIUM, HARD, EXTREME
}

@Entity(tableName = "alarms")
@TypeConverters(AlarmTypeConverters::class)
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
    val daysOfWeek: Set<Int> = emptySet(), // 1=Sunday, ..., 7=Saturday
    val label: String = "",
    val soundUri: String? = null,
    val challengeTypes: Set<ChallengeType> = setOf(ChallengeType.NONE),
    val mathDifficulty: MathDifficulty = MathDifficulty.EASY,
    val barcodeValue: String? = null,
    val isVolumeBoostEnabled: Boolean = false,
    val wakeUpCheckMinutes: Int = 0,  // 0 = disabled, or 1/2/5/10/15/30 minutes
    val rewriteText: String = "",
    val stepCount: Int = 30,
    val sinkImageUri: String? = null,       // Reference image URI for scan sink challenge
    val scanObjectLabel: String = ""         // Selected object label for scan object challenge
)

class AlarmTypeConverters {
    @TypeConverter
    fun fromString(value: String): Set<Int> {
        if (value.isBlank() || value == "[]") return emptySet()
        return value
            .removeSurrounding("[", "]")
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }

    @TypeConverter
    fun fromSet(set: Set<Int>): String {
        return set.joinToString(",", "[", "]")
    }

    @TypeConverter
    fun fromChallengeTypeSet(set: Set<ChallengeType>): String {
        return set.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toChallengeTypeSet(value: String): Set<ChallengeType> {
        if (value.isBlank()) return setOf(ChallengeType.NONE)
        return value.split(",")
            .mapNotNull { 
                try { ChallengeType.valueOf(it.trim()) } 
                catch (e: IllegalArgumentException) { null }
            }
            .toSet()
            .ifEmpty { setOf(ChallengeType.NONE) }
    }
}
