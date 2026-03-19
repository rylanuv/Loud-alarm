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
        return Room.databaseBuilder(
            context,
            AlarmDatabase::class.java,
            "alarm_database"
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
        .build()
    }

    @Provides
    fun provideAlarmDao(database: AlarmDatabase): AlarmDao {
        return database.alarmDao()
    }
}
