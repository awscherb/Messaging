package com.awscherb.messaging.worker

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.database.getStringOrNull
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.awscherb.messaging.ThreadDao
import com.awscherb.messaging.data.MessageThread
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class ThreadImportWorker(
    private val context: Context,
    workerParameters: WorkerParameters,
    private val threadDao: ThreadDao
) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        println("Starting ThreadImportWorker...")

        val savedThreadCount = threadDao.getLastUpdated().associate { it.threadId to it.date }

        val contentResolver = context.contentResolver

        // thread id - time
        val deviceMessageMap = mutableMapOf<String, Long>()
        val toUpsert = mutableSetOf<String>()
        val toDelete = mutableSetOf<String>()

        contentResolver.query(
            // complete-conversations is every message
            // this query does
            Uri.parse("content://mms-sms/conversations?simple=true"),
            arrayOf("_id", "date"),
            null,
            null,
            "DATE DESC"
        )?.use {
            it.moveToFirst()
            while (!it.isAfterLast) {
                val threadId = it.getString(it.getColumnIndexOrThrow("_id"))
                val date = it.getLong(it.getColumnIndexOrThrow("date"))

                if (threadId !in savedThreadCount) {
                    toUpsert += threadId
                }

                deviceMessageMap[threadId] = date
                it.moveToNext()
            }
        }

        savedThreadCount.forEach { (threadId, lastUpdate) ->
            if ((deviceMessageMap[threadId] ?: 0L) > lastUpdate) {
                // dev is newer than saved
                toUpsert.add(threadId)
            }
        }
        println("Need to update ${toUpsert.size}")

        if (toUpsert.size == 0) {
            return Result.success()
        }


        val msg = mutableListOf<MessagePartial>()
        val recipToLookup = mutableSetOf<String>()
        // https://android.googlesource.com/platform/packages/providers/TelephonyProvider/+/4b14c35/src/com/android/providers/telephony/MmsSmsProvider.java
        contentResolver.query(
            // complete-conversations is every message
            // this query does
            Uri.parse("content://mms-sms/conversations?simple=true"),
            arrayOf("_id", "snippet", "type", "recipient_ids", "date"),
            null,
            null,
            "DATE DESC"
        )?.use {
            println("Simple convo count ${it.count}")
            it.moveToFirst()
            while (!it.isAfterLast) {
                val id = it.getLong(it.getColumnIndexOrThrow("_id"))
                if (id.toString() !in toUpsert) {
                    it.moveToNext()
                    continue
                }
                val message = it.getStringOrNull(it.getColumnIndexOrThrow("snippet")) ?: "MMS"
                val type = it.getInt(it.getColumnIndexOrThrow("type"))
                val recip = it.getString(it.getColumnIndexOrThrow("recipient_ids")).split(" ")
                val date = it.getLong(it.getColumnIndexOrThrow("date"))
                recipToLookup += recip

                msg += MessagePartial(id.toString(), recip, message, date, type)
                if (msg.size == toUpsert.size) {
                    break
                }

                it.moveToNext()
            }
        }

        val recipAddrMap = mutableMapOf<String, String>()
        val recipContactMap = mutableMapOf<String, String>()
        recipToLookup.forEach { recip ->
            // recipient_id -> canonical-address -> contact lookup
            if (recipAddrMap[recip] == null) {
                contentResolver.query(
                    Uri.parse("content://mms-sms/canonical-addresses"),
                    arrayOf("address"),
                    "_id=${recip}",
                    null, null
                )?.use {
                    it.moveToFirst()
                    val addr = it.getString(it.getColumnIndexOrThrow("address"))
                    recipAddrMap[recip] = addr
                }
            }
        }

        val threadMessageIdMap = mutableMapOf<String, String>()
        msg.forEach { msg ->
            if (msg.type == 1) {
                contentResolver.query(
                    Uri.parse("content://mms"),
                    arrayOf("_id"),
                    "thread_id=${msg.id}",
                    null,
                    "date DESC"
                )?.use {
                    it.moveToFirst()
                    if (!it.isAfterLast) {
                        val first = it.getString(it.getColumnIndexOrThrow("_id"))
                        threadMessageIdMap[msg.id] = first
                    }

                }
            }
        }

        val threadIdBodyMap = mutableMapOf<String, String>()
        threadMessageIdMap.forEach { threadId, messageId ->
            val selectionPart = "mid=$messageId"
            val uri = Uri.parse("content://mms/part")
            contentResolver.query(
                uri, arrayOf("_id", "ct", "_data", "text"),
                selectionPart, null, null
            )?.use {

                if (it.moveToFirst()) {
                    do {
                        val partId = it.getString(it.getColumnIndexOrThrow("_id"))
                        val type = it.getString(it.getColumnIndexOrThrow("ct"))
                        if ("text/plain" == type) {
                            val data = it.getString(it.getColumnIndexOrThrow("_data"))
                            val body: String = if (data != null) {
                                // implementation of this method below
                                getMmsText(partId)
                            } else {
                                it.getString(it.getColumnIndexOrThrow("text"))
                            }
                            threadIdBodyMap[threadId] = body
                        } else if ("image/jpeg" == type) {
                            threadIdBodyMap[threadId] = "Image"
                            continue
                        }
                    } while (it.moveToNext())
                }
            }
        }

        recipAddrMap.forEach { recip, addr ->
            contentResolver.query(
                Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, addr),
                arrayOf("display_name"),
                null,
                null,
                null
            )?.use {
                it.moveToFirst()
                if (!it.isAfterLast) {
                    val gotten = it.getString(it.getColumnIndexOrThrow("display_name"))
                    recipContactMap[recip] = gotten
                }
            }
        }

        val threads = msg.map {
            MessageThread(
                threadId = it.id,
                participants = it.recipients.map { recipId ->
                    recipContactMap[recipId] ?: recipAddrMap[recipId] ?: ""
                },
                message = if (it.type == 0) it.message else threadIdBodyMap[it.id] ?: "",
                date = it.date
            )
        }

        threads.chunked(100).forEach { chunk ->
            threadDao.insertAll(chunk)
        }

        return Result.success()
    }

    private fun getMmsText(id: String): String {
        val partURI = Uri.parse("content://mms/part/$id")
        var stream: InputStream? = null
        val sb = StringBuilder()
        try {
            stream = context.contentResolver.openInputStream(partURI)
            if (stream != null) {
                val isr = InputStreamReader(stream, "UTF-8")
                val reader = BufferedReader(isr)
                var temp = reader.readLine()
                while (temp != null) {
                    sb.append(temp)
                    temp = reader.readLine()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (stream != null) {
                try {
                    stream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return sb.toString()
    }
}

data class MessagePartial(
    val id: String,
    val recipients: List<String>,
    val message: String,
    val date: Long,
    val type: Int // 0 = sms, 1 = mms
)
