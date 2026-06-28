package com.loud.alarm.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.loud.alarm.data.AlarmDao
import com.loud.alarm.data.AlarmDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAlarmDatabase(@ApplicationContext context: Context): AlarmDatabase {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN isVolumeBoostEnabled INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `alarms_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `hour` INTEGER NOT NULL, 
                        `minute` INTEGER NOT NULL, 
                        `enabled` INTEGER NOT NULL, 
                        `daysOfWeek` TEXT NOT NULL, 
                        `label` TEXT NOT NULL, 
                        `soundUri` TEXT, 
                        `challengeType` TEXT NOT NULL, 
                        `mathDifficulty` TEXT NOT NULL, 
                        `barcodeValue` TEXT, 
                        `isVolumeBoostEnabled` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO alarms_new (id, hour, minute, enabled, daysOfWeek, label, soundUri, challengeType, mathDifficulty, barcodeValue, isVolumeBoostEnabled)
                    SELECT id, hour, minute, enabled, daysOfWeek, label, soundUri, challengeType, mathDifficulty, barcodeValue, isVolumeBoostEnabled FROM alarms
                """.trimIndent())
                db.execSQL("DROP TABLE alarms")
                db.execSQL("ALTER TABLE alarms_new RENAME TO alarms")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Rename BARCODE enum values to QR_CODE
                db.execSQL("UPDATE alarms SET challengeType = 'QR_CODE' WHERE challengeType = 'BARCODE'")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN wakeUpCheckMinutes INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN rewriteText TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE alarms ADD COLUMN stepCount INTEGER NOT NULL DEFAULT 30")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Rename challengeType -> challengeTypes and convert BOTH -> MATH,QR_CODE
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `alarms_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `hour` INTEGER NOT NULL, 
                        `minute` INTEGER NOT NULL, 
                        `enabled` INTEGER NOT NULL, 
                        `daysOfWeek` TEXT NOT NULL, 
                        `label` TEXT NOT NULL, 
                        `soundUri` TEXT, 
                        `challengeTypes` TEXT NOT NULL, 
                        `mathDifficulty` TEXT NOT NULL, 
                        `barcodeValue` TEXT, 
                        `isVolumeBoostEnabled` INTEGER NOT NULL,
                        `wakeUpCheckMinutes` INTEGER NOT NULL DEFAULT 0,
                        `rewriteText` TEXT NOT NULL DEFAULT '',
                        `stepCount` INTEGER NOT NULL DEFAULT 30
                    )
                """.trimIndent())
                // Convert old single challengeType to new challengeTypes set format
                // BOTH becomes MATH,QR_CODE
                db.execSQL("""
                    INSERT INTO alarms_new (id, hour, minute, enabled, daysOfWeek, label, soundUri, challengeTypes, mathDifficulty, barcodeValue, isVolumeBoostEnabled, wakeUpCheckMinutes, rewriteText, stepCount)
                    SELECT id, hour, minute, enabled, daysOfWeek, label, soundUri, 
                        CASE challengeType 
                            WHEN 'BOTH' THEN 'MATH,QR_CODE' 
                            ELSE challengeType 
                        END, 
                        mathDifficulty, barcodeValue, isVolumeBoostEnabled, wakeUpCheckMinutes, rewriteText, stepCount 
                    FROM alarms
                """.trimIndent())
                db.execSQL("DROP TABLE alarms")
                db.execSQL("ALTER TABLE alarms_new RENAME TO alarms")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN sinkImageUri TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE alarms ADD COLUMN scanObjectLabel TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Normalize difficulty values to supported enum names.
                db.execSQL(
                    """
                    UPDATE alarms
                    SET mathDifficulty = CASE UPPER(TRIM(mathDifficulty))
                        WHEN 'EASY' THEN 'EASY'
                        WHEN 'MEDIUM' THEN 'MEDIUM'
                        WHEN 'HARD' THEN 'HARD'
                        WHEN 'EXTREME' THEN 'EXTREME'
                        WHEN 'NORMAL' THEN 'MEDIUM'
                        WHEN 'ADVANCED' THEN 'HARD'
                        WHEN 'VERY_HARD' THEN 'EXTREME'
                        WHEN 'INSANE' THEN 'EXTREME'
                        ELSE 'EASY'
                    END
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN mazeDifficulty TEXT NOT NULL DEFAULT 'EASY'")
            }
        }
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN shakeCount INTEGER NOT NULL DEFAULT 30")
            }
        }
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE alarms ADD COLUMN scanObjectExcluded TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                    // Column might already exist if device was on an intermediate build
                    android.util.Log.e("DatabaseModule", "Migration 11->12 column add error (likely duplicates)", e)
                }
            }
        }
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN puzzleDifficulty TEXT NOT NULL DEFAULT 'EASY'")
            }
        }
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN squatCount INTEGER NOT NULL DEFAULT 15")
                db.execSQL("ALTER TABLE alarms ADD COLUMN pushUpCount INTEGER NOT NULL DEFAULT 15")
            }
        }
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN reverseTypingCount INTEGER NOT NULL DEFAULT 3")
            }
        }
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN mathQuestionCount INTEGER NOT NULL DEFAULT 1")
            }
        }
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN memoryDifficulty TEXT NOT NULL DEFAULT 'EASY'")
                db.execSQL("ALTER TABLE alarms ADD COLUMN memoryChallengeCount INTEGER NOT NULL DEFAULT 3")
            }
        }
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN pushUpDifficulty TEXT NOT NULL DEFAULT 'HARD'")
            }
        }
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN squatDetectionMode TEXT NOT NULL DEFAULT 'CAMERA'")
            }
        }
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN tapCount INTEGER NOT NULL DEFAULT 30")
            }
        }
        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN spellBeeDifficulty TEXT NOT NULL DEFAULT 'EASY'")
                db.execSQL("ALTER TABLE alarms ADD COLUMN audioMemoryDifficulty TEXT NOT NULL DEFAULT 'EASY'")
            }
        }
        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN spellBeeCount INTEGER NOT NULL DEFAULT 3")
            }
        }
        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN audioMemoryChallengeCount INTEGER NOT NULL DEFAULT 3")
            }
        }
        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN clockReadingDifficulty TEXT NOT NULL DEFAULT 'EASY'")
                db.execSQL("ALTER TABLE alarms ADD COLUMN clockReadingCount INTEGER NOT NULL DEFAULT 1")
            }
        }
        return Room.databaseBuilder(
            context,
            AlarmDatabase::class.java,
            "alarm_database"
        )
        .addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14,
            MIGRATION_14_15,
            MIGRATION_15_16,
            MIGRATION_16_17,
            MIGRATION_17_18,
            MIGRATION_18_19,
            MIGRATION_19_20,
            MIGRATION_20_21,
            MIGRATION_21_22,
            MIGRATION_22_23,
            MIGRATION_23_24
        )
        .build()
    }

    @Provides
    fun provideAlarmDao(database: AlarmDatabase): AlarmDao {
        return database.alarmDao()
    }
}
