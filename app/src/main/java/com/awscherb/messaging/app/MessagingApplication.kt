package com.awscherb.messaging.app

import android.app.Application
import androidx.work.Configuration
import com.awscherb.messaging.ThreadDao
import com.awscherb.messaging.service.ContactService
import com.awscherb.messaging.worker.MessagingWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject


@HiltAndroidApp
class MessagingApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var threadDao: ThreadDao

    @Inject
    lateinit var contactService: ContactService

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .setWorkerFactory(
                MessagingWorkerFactory(
                    threadDao,
                    contactService
                )
            )
            .build()

}