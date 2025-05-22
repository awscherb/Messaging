package com.awscherb.messaging.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.ims.ImsManager
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.awscherb.messaging.worker.ThreadImportWorker
import com.awscherb.messaging.worker.ThreadRecordWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class MessageReceiver : BroadcastReceiver() {

    val scope = CoroutineScope(Dispatchers.IO)
    override fun onReceive(context: Context, intent: Intent) {

        val req = OneTimeWorkRequestBuilder<ThreadImportWorker>()
            .setInputData(workDataOf(ThreadImportWorker.SingleSync to true))
            .build()

        with(WorkManager.getInstance(context)) {
            enqueue(req)

            getWorkInfoByIdFlow(req.id)
                .onEach {
                    when (it?.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            val topThreads = it .outputData.getStringArray(
                                ThreadImportWorker.TOP_THREADS
                            ) ?: emptyArray()

                            val syncMessages = OneTimeWorkRequestBuilder<ThreadRecordWorker>()
                                .setInputData(workDataOf(
                                    ThreadRecordWorker.THREAD_ID to topThreads
                                ))
                                .build()

                            enqueue(syncMessages)

                        }
                        else -> {
                        }
                    }
                }.launchIn(scope)
        }
    }
}