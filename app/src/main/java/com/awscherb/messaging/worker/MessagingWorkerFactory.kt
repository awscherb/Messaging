package com.awscherb.messaging.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.awscherb.messaging.ThreadDao
import com.awscherb.messaging.service.ContactService

class MessagingWorkerFactory(
    private val threadDao: ThreadDao,
    private val contactService: ContactService
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            ThreadImportWorker::class.java.name -> ThreadImportWorker(
                context = appContext,
                contactService = contactService,
                workerParameters = workerParameters,
                threadDao = threadDao
            )

            else -> null
        }
    }
}