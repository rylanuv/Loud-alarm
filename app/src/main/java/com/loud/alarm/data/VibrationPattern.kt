package com.loud.alarm.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Waves
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Defines available vibration patterns for the alarm.
 * Each pattern has a display name and the actual vibration timing array.
 * The timings follow Android's vibration pattern format:
 * [delay, vibrate, sleep, vibrate, sleep, ...]
 * Repeat index 0 means "loop from the start."
 */
enum class VibrationPattern(
    val displayName: String,
    val icon: ImageVector,
    val pattern: LongArray,
    val isPremium: Boolean
) {
    DEVICE_DEFAULT(
        displayName = "Default",
        icon = Icons.Default.PhoneAndroid,
        pattern = longArrayOf(0, 500, 500),
        isPremium = false
    ),
    BREEZE(
        displayName = "Breeze",
        icon = Icons.Default.Spa,
        pattern = longArrayOf(0, 50, 600, 50, 600),
        isPremium = true
    ),
    PULSE(
        displayName = "Pulse",
        icon = Icons.Default.Waves,
        pattern = longArrayOf(0, 200, 400),
        isPremium = true
    ),
    HEARTBEAT(
        displayName = "Heartbeat",
        icon = Icons.Default.Favorite,
        pattern = longArrayOf(0, 120, 100, 120, 800),
        isPremium = true
    ),
    SOS(
        displayName = "SOS",
        icon = Icons.Default.Sos,
        // S (100, 100, 100, 100, 100), 300 gap
        // O (300, 100, 300, 100, 300), 300 gap
        // S (100, 100, 100, 100, 100), 700 gap
        pattern = longArrayOf(0, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100, 700),
        isPremium = true
    ),
    BLAST(
        displayName = "Blast",
        icon = Icons.Default.LocalFireDepartment,
        pattern = longArrayOf(0, 1000, 50, 1000, 50, 1000, 100),
        isPremium = true
    );

    companion object {
        fun fromName(name: String): VibrationPattern {
            return entries.find { it.name == name } ?: DEVICE_DEFAULT
        }
    }
}
