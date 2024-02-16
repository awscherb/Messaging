package com.awscherb.messaging.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.awscherb.messaging.ThreadDao
import com.awscherb.messaging.data.MessageThread

@Database(
    entities = [
        MessageThread::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(MessagingTypeConverters::class)
abstract class MessagingDatabase: RoomDatabase() {
    abstract fun threadDao(): ThreadDao
}