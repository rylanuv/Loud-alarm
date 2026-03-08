# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# ── Room ──
-keep class com.loud.alarm.data.Alarm { *; }
-keep class com.loud.alarm.data.AlarmTypeConverters { *; }
-keep class com.loud.alarm.data.ChallengeType { *; }
-keep class com.loud.alarm.data.MathDifficulty { *; }
-keep class * extends androidx.room.RoomDatabase
-keep class * implements androidx.room.dao.Dao

# ── ML Kit ──
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ── Google Play Billing ──
-keep class com.android.vending.billing.** { *; }
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# ── CameraX ──
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ── Hilt / Dagger ──
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltViewModelFactory
-keepclasseswithmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel *;
}
-keep class com.loud.alarm.di.** { *; }

# ── DataStore ──
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ── Kotlin serialization / coroutines ──
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# ── Keep Compose ──
-dontwarn androidx.compose.**

# ── Keep service / receiver classes ──
-keep class com.loud.alarm.service.** { *; }
-keep class com.loud.alarm.billing.** { *; }
