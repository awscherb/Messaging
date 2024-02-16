package com.awscherb.messaging.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.awscherb.messaging.ThreadDao

class MessagingWorkerFactory(
    private val threadDao: ThreadDao
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            ThreadImportWorker::class.java.name -> ThreadImportWorker(
                appContext,
                workerParameters,
                threadDao
            )

            else -> null
        }
    }
}