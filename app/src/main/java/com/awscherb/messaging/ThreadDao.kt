package com.awscherb.messaging

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.awscherb.messaging.data.MessageThread
import com.awscherb.messaging.data.ThreadLastUpdated
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(thread: MessageThread): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(threads: List<MessageThread>): LongArray

    @Query("SELECT * FROM MessageThread ORDER BY date DESC")
    fun listAllThreads(): Flow<List<MessageThread>>

    @Query("SELECT COUNT(threadId) FROM MessageThread")
    suspend fun countRows(): Int

    @Query("SELECT threadId, date from MessageThread")
    suspend fun getLastUpdated(): List<ThreadLastUpdated>

}