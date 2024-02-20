package com.awscherb.messaging.db

import android.content.Context
import androidx.room.Room
import com.awscherb.messaging.dao.MessageDao
import com.awscherb.messaging.dao.ThreadDao
import com.awscherb.messaging.dao.ThreadMessageRecordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DatabaseModule {

    companion object {

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): MessagingDatabase {
            return Room.databaseBuilder(
                context.applicationContext, MessagingDatabase::class.java, "messaging.db"
            )
                .fallbackToDestructiveMigration()
                .build()
        }

        @Provides
        @Singleton
        fun provideThreadsDao(db: MessagingDatabase): ThreadDao = db.threadDao()

        @Provides
        @Singleton
        fun provideMessageDao(db: MessagingDatabase): MessageDao = db.messageDao()

        @Provides
        @Singleton
        fun provideThreadMessageRecordDao(db: MessagingDatabase): ThreadMessageRecordDao =
            db.threadMessageRecordDao()
    }
}