package com.sleepcontroller.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sleepcontroller.data.db.dao.BlockedAppDao
import com.sleepcontroller.data.db.dao.MorningTaskDao
import com.sleepcontroller.data.db.dao.SleepHistoryDao
import com.sleepcontroller.data.db.dao.SleepScheduleDao
import com.sleepcontroller.data.db.entity.BlockedApp
import com.sleepcontroller.data.db.entity.MorningTask
import com.sleepcontroller.data.db.entity.SleepHistory
import com.sleepcontroller.data.db.entity.SleepSchedule

@Database(
    entities = [
        SleepSchedule::class,
        MorningTask::class,
        BlockedApp::class,
        SleepHistory::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sleepScheduleDao(): SleepScheduleDao
    abstract fun morningTaskDao(): MorningTaskDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun sleepHistoryDao(): SleepHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration strategy: define explicit migrations for each schema change.
         * This preserves user data across app updates unlike fallbackToDestructiveMigration.
         *
         * Example for a future v1→v2 migration:
         * val MIGRATION_1_2 = object : Migration(1, 2) {
         *     override fun migrate(db: SupportSQLiteDatabase) {
         *         db.execSQL("ALTER TABLE sleep_schedule ADD COLUMN vibrationEnabled INTEGER NOT NULL DEFAULT 1")
         *     }
         * }
         */
        private val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            // Add migrations here as schema evolves:
            // MIGRATION_1_2,
            // MIGRATION_2_3,
        )

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sleep_controller_db"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    // Only fall back to destructive migration as a last resort
                    // when no migration path is found (e.g., developer builds)
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
