package com.awscherb.messaging.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.awscherb.messaging.data.ThreadMessageRecord

@Dao
interface ThreadMessageRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: ThreadMessageRecord): Long

    @Query("SELECT * FROM ThreadMessageRecord WHERE threadId = :threadId ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun fetchRecords(threadId: String, limit: Int, offset: Int): List<ThreadMessageRecord>

    @Query("SELECT COUNT(id) from ThreadMessageRecord")
    suspend fun countRows(): Int

    @Query("SELECT * FROM ThreadMessageRecord WHERE threadId = :threadId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestRecordForThread(threadId: String): ThreadMessageRecord?
}