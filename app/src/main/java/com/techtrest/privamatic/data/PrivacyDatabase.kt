package com.techtrest.privamatic.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.techtrest.privamatic.data.model.PrivacySnapshot

@Database(entities = [PrivacySnapshot::class], version = 1, exportSchema = false)
abstract class PrivacyDatabase : RoomDatabase() {
    abstract fun snapshotDao(): PrivacySnapshotDao

    companion object {
        @Volatile private var INSTANCE: PrivacyDatabase? = null

        fun getInstance(context: Context): PrivacyDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PrivacyDatabase::class.java,
                    "privacy_history.db"
                )
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .build().also { INSTANCE = it }
            }
    }
}
