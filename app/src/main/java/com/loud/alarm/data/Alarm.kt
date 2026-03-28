package com.loud.alarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.Locale

enum class ChallengeType {
    NONE, MATH, QR_CODE, REWRITE, STEP, MAZE, MEMORY, SHAKE, SPELL_BEE, PUZZLE, SCAN_SINK, SCAN_OBJECT
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
    val mazeDifficulty: MathDifficulty = MathDifficulty.EASY,
    val barcodeValue: String? = null,
    val isVolumeBoostEnabled: Boolean = false,
    val wakeUpCheckMinutes: Int = 0,  // 0 = disabled, or 1/2/5/10/15/30 minutes
    val rewriteText: String = "",
    val stepCount: Int = 30,
    val shakeCount: Int = 30,
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
                val trimmed = it.trim()
                // Backward compatibility: map old "TYPING" to new SPELL_BEE
                val mapped = if (trimmed == "TYPING") "SPELL_BEE" else trimmed
                try { ChallengeType.valueOf(mapped) } 
                catch (e: IllegalArgumentException) { null }
            }
            .toSet()
            .ifEmpty { setOf(ChallengeType.NONE) }
    }

    @TypeConverter
    fun fromMathDifficulty(value: MathDifficulty): String {
        return value.name
    }

    @TypeConverter
    fun toMathDifficulty(value: String?): MathDifficulty {
        if (value.isNullOrBlank()) return MathDifficulty.EASY
        return when (value.trim().uppercase(Locale.ROOT)) {
            "EASY" -> MathDifficulty.EASY
            "MEDIUM", "NORMAL" -> MathDifficulty.MEDIUM
            "HARD", "ADVANCED" -> MathDifficulty.HARD
            "EXTREME", "VERY_HARD", "INSANE" -> MathDifficulty.EXTREME
            else -> MathDifficulty.EASY
        }
    }
}
