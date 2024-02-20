package com.awscherb.messaging.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.awscherb.messaging.data.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM Message WHERE threadId = :threadId ORDER BY date DESC")
    fun listMessagesForThread(threadId: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(message: List<Message>): LongArray
}