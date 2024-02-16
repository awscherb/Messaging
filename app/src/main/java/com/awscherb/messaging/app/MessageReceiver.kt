package com.awscherb.messaging.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.awscherb.messaging.worker.ThreadImportWorker


class MessageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        println("MessageReceiverd recevied $intent")

        val req = OneTimeWorkRequestBuilder<ThreadImportWorker>()
            .build()

        with(WorkManager.getInstance(context)) {
            enqueue(req)
        }
    }
}