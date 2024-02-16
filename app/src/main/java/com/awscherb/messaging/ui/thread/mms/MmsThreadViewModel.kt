package com.awscherb.messaging.ui.thread.mms

import android.app.Application
import android.content.Context
import android.database.DatabaseUtils
import android.net.Uri
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.awscherb.messaging.service.ContactService
import com.awscherb.messaging.ui.thread.common.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject

@HiltViewModel
class MmsThreadViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    contactService: ContactService,
    @ApplicationContext context: Context
) : AndroidViewModel(context as Application) {
    val threadId = savedStateHandle.get<String>("id")

    val messages = MutableStateFlow<List<Message>>(emptyList())

    init {
        viewModelScope.launch {

            val messageIds = mutableListOf<String>()
            val mIdBox = mutableMapOf<String, Int>()
            context.contentResolver.query(
                Uri.parse("content://mms"),
                null,
                "thread_id=$threadId",
                null, "DATE DESC LIMIT 10"
            )?.use {
                println("In query ${it.count}")
                DatabaseUtils.dumpCursor(it)
                it.moveToFirst()
                while (!it.isAfterLast) {
                    val id = it.getString(it.getColumnIndexOrThrow("_id"))
                    val box = it.getInt(it.getColumnIndexOrThrow("msg_box"))
                    messageIds += id
                    mIdBox[id] = box
                    it.moveToNext()
                }
            }

            val mIdBody = mutableMapOf<String, MutableList<String>>()
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
                    DatabaseUtils.dumpCursor(it)
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
                    DatabaseUtils.dumpCursor(it)
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


                            else -> {
                                mIdBody[mid] =
                                    mIdBody.getOrDefault(mid, mutableListOf())
                                        .also { it.add("Media") }
                            }
                        }
                        it.moveToNext()
                    }
                }
            }

            val msglist = mutableListOf<Message>()
            mIdFromContact.forEach { (mId, contact) ->
                msglist += Message(
                    mId, mIdBody[mId]?.lastOrNull() ?: "empty",
                    mIdBox[mId] == 2, contact
                )
            }
            messages.value = msglist
        }
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