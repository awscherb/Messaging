package com.awscherb.messaging.content

import android.content.Context
import android.database.DatabaseUtils
import android.net.Uri
import com.awscherb.messaging.service.ContactService
import com.awscherb.messaging.ui.thread.common.Message
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

object MmsHelper {

    suspend fun getMessagesForMms(
        context: Context,
        ids: List<String>,
        contactService: ContactService,
    ): List<Message> {

        val messageIds = mutableListOf<String>()
        val mIdBox = mutableMapOf<String, Int>()
        val mIdDate = mutableMapOf<String, Long>()

        context.contentResolver.query(
            Uri.parse("content://mms"),
            null,
            "_id IN (${ids.joinToString(separator = ",")})",
            null,
            null
        )?.use {
            it.moveToFirst()
            while (!it.isAfterLast) {
                val id = it.getString(it.getColumnIndexOrThrow("_id"))
                val box = it.getInt(it.getColumnIndexOrThrow("msg_box"))
                var date = it.getLong(it.getColumnIndexOrThrow("date"))

                if (date < 1000000000000) {
                    date *= 1000
                }

                messageIds += id
                mIdBox[id] = box
                mIdDate[id] = date
                it.moveToNext()
            }
        }

        val mIdBody = mutableMapOf<String, MutableList<String>>()
        val mIdData = mutableMapOf<String, String>()
        val mIdAddr = mutableMapOf<String, String>()
        val mIdFromContact = mutableMapOf<String, String>()

        messageIds.forEach { mid ->
            context.contentResolver.query(
                Uri.parse("content://mms/$mid/addr"),
                null,
                null,
                null,
                null
            )?.use {
                it.moveToFirst()
                while (!it.isAfterLast) {
                    val type = it.getInt(it.getColumnIndexOrThrow("type"))
                    if (type == 137) {
                        val address = it.getString(it.getColumnIndexOrThrow("address"))
                        mIdAddr[mid] = address
                        break
                    }
                    it.moveToNext()
                }
            }
        }

        mIdAddr.forEach { (mId, addr) ->
            mIdFromContact[mId] = contactService.fetchContact(addr)?.displayName ?: ""
        }

        val selectionQuery = "mid IN (${messageIds.joinToString(",")})"
        context.contentResolver.query(
            Uri.parse("content://mms/part"),
            null,
            selectionQuery,
            null, null
        )?.use {
            if (it.moveToFirst()) {
                var body: String?
                while (!it.isAfterLast) {
                    val mid = it.getString(it.getColumnIndexOrThrow("mid"))
                    val partId = it.getString(it.getColumnIndexOrThrow("_id"))
                    when (it.getString(it.getColumnIndexOrThrow("ct"))) {
                        "text/plain" -> {
                            val data = it.getString(it.getColumnIndexOrThrow("_data"))
                            body = if (data != null) {
                                // implementation of this method below
                                getMmsText(partId, context)
                            } else {
                                it.getString(it.getColumnIndexOrThrow("text"))
                            }
                            mIdBody[mid] = mIdBody.getOrDefault(mid, mutableListOf())
                                .also { it.add(body ?: "empty") }
                        }

                        "image/jpeg" -> {
                            mIdData[mid]  = partId
                        }


                        else -> {
                            mIdBody[mid] =
                                mIdBody.getOrDefault(mid, mutableListOf())
                                    .also { it.add("") }
                        }
                    }
                    it.moveToNext()
                }
            }
        }

        val msglist = mutableListOf<Message>()
        mIdFromContact.forEach { (mId, contact) ->
            msglist += Message(
                id = mId,
                text = mIdBody[mId]?.lastOrNull() ?: "empty",
                fromMe = mIdBox[mId] == 2,
                contact = contact,
                date = mIdDate[mId] ?: 0L,
                data = mIdData[mId]
            )
        }
        return msglist
    }

    private fun getMmsText(id: String, context: Context): String {
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