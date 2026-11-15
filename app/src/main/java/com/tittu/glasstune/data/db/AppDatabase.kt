package com.tittu.glasstune.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PlaylistEntity::class, PlaylistSongEntity::class, EqPresetEntity::class, LyricsEntity::class, PlayHistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        /** Add play history without touching existing tables (user data preserved). */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `play_history` (" +
                        "`songPath` TEXT NOT NULL PRIMARY KEY, " +
                        "`songId` INTEGER NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, " +
                        "`playCount` INTEGER NOT NULL, `lastPlayed` INTEGER NOT NULL)"
                )
            }
        }

        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, AppDatabase::class.java, "glasstune.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}