package com.awscherb.messaging.worker

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.awscherb.messaging.dao.ThreadMessageRecordDao
import com.awscherb.messaging.data.MessageType
import com.awscherb.messaging.data.ThreadMessageRecord
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.withContext

class ThreadRecordWorker(
    private val context: Context,
    private val threadMessageRecordDao: ThreadMessageRecordDao,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {

    companion object {
        const val THREAD_ID = "thread_id"
    }

    override suspend fun doWork(): Result {
        val threadIds = inputData.getStringArray(THREAD_ID) ?: return Result.failure()
        println("Syncing for ${threadIds.map { it }.joinToString(",")}")

        val sync = mutableListOf<Deferred<Result>>()
        threadIds.forEach { threadId ->
            val latestThread = threadMessageRecordDao.getLatestRecordForThread(threadId)
            println("latest thread $latestThread")
            coroutineScope {
                sync += async {
                    withContext(Dispatchers.IO) {
                        val start = System.currentTimeMillis()
                        println("Syncing records for thread $threadId")
                        context.contentResolver.query(
                            Uri.parse("content://mms-sms/conversations/$threadId"),
                            arrayOf("_id", "date", "thread_id", "transport_type"),
                            null,
                            null,
                            null
                        )?.use {
                            it.moveToFirst()
                            var i = 0
                            while (!it.isAfterLast) {
                                val id = it.getString(it.getColumnIndexOrThrow("_id"))
                                val transportType =
                                    it.getString(it.getColumnIndexOrThrow("transport_type"))
                                var date = it.getLong(it.getColumnIndexOrThrow("date"))

                                // mms table uses seconds since epoch, convert all to millis
                                if (date < 1000000000000) {
                                    date *= 1000
                                }

                                if (date < (latestThread?.date ?: 0)) {
                                    it.moveToNext()
                                    continue
                                }

                                val type: MessageType =
                                    if (transportType == "sms") MessageType.SMS else MessageType.MMS

                                val record = ThreadMessageRecord(
                                    id = id,
                                    date = date,
                                    threadId = threadId,
                                    type = type
                                )

                                val res = threadMessageRecordDao.insertRecords(record)

                                i++
                                it.moveToNext()
                            }

                            println("Completed sync for thread $threadId in ${System.currentTimeMillis() - start}")
                            Result.success()
                        } ?: Result.failure()
                    }
                }
            }
        }
        sync.joinAll()
        return Result.success()
    }
}