package com.awscherb.messaging.worker

import android.content.Context
import android.database.DatabaseUtils
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.database.getStringOrNull
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.awscherb.messaging.ThreadDao
import com.awscherb.messaging.data.MessageThread
import com.awscherb.messaging.data.MessageType
import com.awscherb.messaging.service.ContactService
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class ThreadImportWorker(
    private val context: Context,
    private val contactService: ContactService,
    workerParameters: WorkerParameters,
    private val threadDao: ThreadDao
) : CoroutineWorker(context, workerParameters) {

    companion object {

        const val ThreadsFullSync = "threads_full_sync"

        const val Step = "step"
        const val CheckingForUpdates = "Checking for updates"
        const val FetchingThreads = "Fetching threads"
        const val ResolvingAddresses = "Resolving addresses"
        const val FetchingThreadData = "Fetching thread data"
        const val FetchingMessagesForThreads = "Fetching messages for threads"
        const val ResolvingContacts = "Resolving contacts"
        const val SavingData = "Saving data"

        fun shouldLookupParts(messageType: Int, snippet: String?): Boolean {
            return messageType == 0 && snippet.isNullOrBlank()
        }

        private const val SNIPPET_FROM_ME = 2

    }


    override suspend fun doWork(): Result {
        println("Starting ThreadImportWorker...")

        val savedThreadCount = threadDao.getLastUpdated().associate { it.threadId to it.date }

        val contentResolver = context.contentResolver

        // thread id - time
        val deviceMessageMap = mutableMapOf<String, Long>()
        val toUpsert = mutableSetOf<String>()
        // todo
        val toDelete = mutableSetOf<String>()

        setProgress(workDataOf(Step to CheckingForUpdates))
        contentResolver.query(
            // complete-conversations is every message
            // this query does
            Uri.parse("content://mms-sms/conversations?simple=true"),
            arrayOf("_id", "date", "message_count"),
            null,
            null,
            "DATE DESC"
        )?.use {
            it.moveToFirst()
            while (!it.isAfterLast) {
                val threadId = it.getString(it.getColumnIndexOrThrow("_id"))
                val date = it.getLong(it.getColumnIndexOrThrow("date"))
                val messageCount = it.getInt(it.getColumnIndexOrThrow("message_count"))

                if (messageCount == 0) {
                    it.moveToNext()
                    continue
                }

                if (threadId !in savedThreadCount.keys) {
                    println("threadId $threadId ${savedThreadCount.keys}")
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

        if (toUpsert.size == 0) {
            Log.i("ThreadImportWorker", "All threads up to date")
            return Result.success()
        }

        setProgress(workDataOf(Step to FetchingThreads))
        val msg = mutableListOf<MessagePartial>()
        val recipToLookup = mutableSetOf<String>()
        // https://android.googlesource.com/platform/packages/providers/TelephonyProvider/+/4b14c35/src/com/android/providers/telephony/MmsSmsProvider.java
        contentResolver.query(
            // complete-conversations is every message
            // this query does
            Uri.parse("content://mms-sms/conversations?simple=true"),
            null,
            null,
            null,
            "DATE DESC"
        )?.use {
            it.moveToFirst()
            while (!it.isAfterLast) {
                // RCS has blank "" snippet and snippet type 1
                val id = it.getLong(it.getColumnIndexOrThrow("_id"))
                if (id.toString() !in toUpsert) {
                    it.moveToNext()
                    continue
                }
                val messageCount = it.getInt(it.getColumnIndexOrThrow("message_count"))
                if (messageCount == 0) {
                    it.moveToNext()
                    continue
                }

                val message = it.getStringOrNull(it.getColumnIndexOrThrow("snippet"))
                var type = it.getInt(it.getColumnIndexOrThrow("type"))
                // 1 - from someone else, 2 - from me
                val snippetType = it.getInt(it.getColumnIndexOrThrow("snippet_type"))
                val read = it.getInt(it.getColumnIndexOrThrow("read"))
                val recip = it.getString(it.getColumnIndexOrThrow("recipient_ids")).split(" ")
                val date = it.getLong(it.getColumnIndexOrThrow("date"))
                recipToLookup += recip

                if (shouldLookupParts(type, message)) {
                    // Some messages (RCS) are type 0 (text) but have data in the mms parts table
                    type = 1
                }

                msg += MessagePartial(
                    id = id.toString(),
                    recipients = recip,
                    message = message ?: "",
                    date = date,
                    type = type,
                    fromMe = snippetType == SNIPPET_FROM_ME,
                    read = read == 1
                )
                if (msg.size == toUpsert.size) {
                    break
                }

                it.moveToNext()
            }
        }

        setProgress(workDataOf(Step to ResolvingAddresses))
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

        setProgress(workDataOf(Step to FetchingThreadData))
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

        // thread Id - body from mms part table
        val threadIdBodyMap = mutableMapOf<String, String>()
        setProgress(workDataOf(Step to FetchingMessagesForThreads))
        threadMessageIdMap.forEach { threadId, messageId ->
            val selectionPart = "mid=$messageId"
            val uri = Uri.parse("content://mms/part")
            contentResolver.query(
                uri, arrayOf("_id", "ct", "_data", "text"),
                selectionPart, null, null
            )?.use {
                if (it.moveToFirst()) {
                    var body: String? = null
                    var mediaType: String? = null
                    while (!it.isAfterLast) {
                        val partId = it.getString(it.getColumnIndexOrThrow("_id"))
                        when (it.getString(it.getColumnIndexOrThrow("ct"))) {
                            "text/plain" -> {
                                val data = it.getString(it.getColumnIndexOrThrow("_data"))
                                body = if (data != null) {
                                    // implementation of this method below
                                    getMmsText(partId)
                                } else {
                                    it.getString(it.getColumnIndexOrThrow("text"))
                                }
                            }

                            "image/jpeg" -> {
                                mediaType = "Image"
                            }

                            "application/smil" -> {
                                mediaType = "Unknown"
                            }

                            else -> {
                                mediaType = "Media"
                            }
                        }
                        it.moveToNext()
                    }
                    threadIdBodyMap[threadId] = body ?: mediaType ?: ""
                }
            }
        }

        setProgress(workDataOf(Step to ResolvingContacts))
        recipAddrMap.forEach { (recip, addr) ->
            recipContactMap[recip] = contactService.fetchContact(addr)?.displayName ?: ""
        }

        val threads = msg.map {
            MessageThread(
                threadId = it.id,
                participants = it.recipients.map { recipId ->
                    recipContactMap[recipId] ?: recipAddrMap[recipId] ?: ""
                },
                message = if (it.type == 0) it.message else threadIdBodyMap[it.id] ?: "",
                date = it.date,
                fromMe = it.fromMe,
                read = it.read,
                threadType = if (it.type == 0) MessageType.SMS else MessageType.MMS
            )
        }

        setProgress(workDataOf(Step to SavingData))
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
    val type: Int, // 0 = sms, 1 = mms,
    val fromMe: Boolean,
    val read: Boolean
)
