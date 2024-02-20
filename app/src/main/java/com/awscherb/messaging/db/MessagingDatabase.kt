package com.awscherb.messaging.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.awscherb.messaging.dao.MessageDao
import com.awscherb.messaging.dao.ThreadDao
import com.awscherb.messaging.dao.ThreadMessageRecordDao
import com.awscherb.messaging.data.Message
import com.awscherb.messaging.data.MessageThread
import com.awscherb.messaging.data.ThreadMessageRecord

@Database(
    entities = [
        MessageThread::class,
        Message::class,
        ThreadMessageRecord::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(MessagingTypeConverters::class)
abstract class MessagingDatabase : RoomDatabase() {
    abstract fun threadDao(): ThreadDao

    abstract fun messageDao(): MessageDao

    abstract fun threadMessageRecordDao(): ThreadMessageRecordDao
}