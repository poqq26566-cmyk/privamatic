package com.techtrest.privamatic.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.techtrest.privamatic.data.model.PrivacySnapshot

@Dao
interface PrivacySnapshotDao {
    @Insert
    suspend fun insert(snapshot: PrivacySnapshot)

    @Query("SELECT * FROM privacy_snapshots WHERE timestamp >= :since ORDER BY timestamp ASC")
    suspend fun getSnapshotsSince(since: Long): List<PrivacySnapshot>

    @Query("SELECT * FROM privacy_snapshots ORDER BY timestamp ASC")
    suspend fun getAllSnapshots(): List<PrivacySnapshot>

    @Query("SELECT COUNT(*) FROM privacy_snapshots WHERE timestamp >= :dayStart AND timestamp < :dayEnd")
    suspend fun countSnapshotsForDay(dayStart: Long, dayEnd: Long): Int

    @Query("DELETE FROM privacy_snapshots")
    suspend fun deleteAll()

    @Query("SELECT MIN(timestamp) FROM privacy_snapshots")
    suspend fun getEarliestTimestamp(): Long?
}
