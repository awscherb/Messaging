package com.awscherb.messaging.content

import android.content.Context
import android.net.Uri
import com.awscherb.messaging.service.ContactService
import com.awscherb.messaging.ui.thread.Message
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MmsHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactService: ContactService
) {

    suspend fun getMessagesForMms(
        ids: List<String>,
    ): List<Message> {

        val messageIdPartial = mutableMapOf<String,MmsPartial>()

        val messageIds = mutableListOf<String>()

        val mmsStart = System.currentTimeMillis()
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
                messageIdPartial[id] =
                    messageIdPartial.getOrDefault(id, MmsPartial(id)).copy(
                        fromMe = box == 2,
                        date = date
                    )

                it.moveToNext()
            }
        }
        println("mms took ${System.currentTimeMillis() - mmsStart}")

        // message ID to address - we use this to lookup contact from address
        val mIdAddr = mutableMapOf<String, String>()
        val addrStart = System.currentTimeMillis()
        messageIds.forEach { mid ->
            // most expensive query
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
        println("Addr took ${System.currentTimeMillis() - addrStart}")

        val contactStart = System.currentTimeMillis()
        mIdAddr.forEach { (mId, addr) ->
            val contact = contactService.fetchContact(addr)?.displayName ?: ""
            messageIdPartial[mId] = messageIdPartial.getOrDefault(mId, MmsPartial(mId)).copy(
                contact = contact
            )
        }
        println("Contact lookup too ${System.currentTimeMillis() - contactStart}")

        val partStat = System.currentTimeMillis()
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
                                getMmsText(partId, context)
                            } else {
                                it.getString(it.getColumnIndexOrThrow("text"))
                            }
                            messageIdPartial[mid] = messageIdPartial.getOrDefault(mid, MmsPartial(mid)).copy(
                                body = body
                            )
                        }

                        "image/jpeg" -> {
                            messageIdPartial[mid] = messageIdPartial.getOrDefault(mid, MmsPartial(mid)).copy(
                                data = partId
                            )
                        }

                        else -> {
                            // maybe do somethind
                        }
                    }
                    it.moveToNext()
                }
            }
        }
        println("Parts took ${System.currentTimeMillis() - partStat}")

        val msglist = mutableListOf<Message>()
        messageIdPartial.forEach { (mId, partial) ->
            msglist += Message(
                id = mId,
                text = partial.body ?: "empty",
                fromMe = partial.fromMe == true,
                contact = partial.contact,
                date = partial.date ?: 0L,
                data = partial.data
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

    data class MmsPartial(
        val id: String,
        val fromMe: Boolean? = null,
        val contact: String? = null,
        val body: String? = null,
        val date: Long? = null,
        val data: String? = null
    )

}