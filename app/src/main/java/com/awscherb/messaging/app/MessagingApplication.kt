package com.awscherb.messaging.app

import android.app.Application
import androidx.work.Configuration
import com.awscherb.messaging.dao.ThreadDao
import com.awscherb.messaging.dao.ThreadMessageRecordDao
import com.awscherb.messaging.service.ContactService
import com.awscherb.messaging.worker.MessagingWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltAndroidApp
class MessagingApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var threadDao: ThreadDao

    @Inject
    lateinit var threadMessageRecordDao: ThreadMessageRecordDao

    @Inject
    lateinit var contactService: ContactService

    override fun onCreate() {
        super.onCreate()
        GlobalScope.launch {
            println("total rows ${threadMessageRecordDao.countRows()}")
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .setWorkerFactory(
                MessagingWorkerFactory(
                    threadDao = threadDao,
                    threadMessageRecordDao = threadMessageRecordDao,
                    contactService = contactService
                )
            )
            .build()

}