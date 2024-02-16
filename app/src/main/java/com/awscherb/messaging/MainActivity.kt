package com.awscherb.messaging

import android.Manifest
import android.content.pm.PackageManager
import android.database.DatabaseUtils
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.database.getStringOrNull
import androidx.lifecycle.coroutineScope
import com.awscherb.messaging.ui.messages.MessagesScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {

    private val smsState = MutableStateFlow<List<MessageThread>>(emptyList())
    private val page = MutableStateFlow(0)

    companion object {
        private val Permissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS
        )
    }

    private val request = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (it.all { (_, v) -> v }) {
            // readSms()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        page.onEach {
            // readSms(offset = it)
        }.launchIn(lifecycle.coroutineScope)

        if (Permissions.any {
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }) {
            request.launch(Permissions)
        }

        readMms()
        setContent {
            val messages by smsState.collectAsState()
            MessagesScreen(
                messages = messages
            ) {
                page.value = page.value + 1
            }
        }
    }

    private fun readMms() {
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
            var i = 0
            it.moveToFirst()
            while (i < 20) {
                val id = it.getLong(it.getColumnIndexOrThrow("_id"))
                val message = it.getStringOrNull(it.getColumnIndexOrThrow("snippet")) ?: "MMS"
                val type = it.getInt(it.getColumnIndexOrThrow("type"))
                val recip = it.getString(it.getColumnIndexOrThrow("recipient_ids")).split(" ")
                val date = it.getLong(it.getColumnIndexOrThrow("date"))
                recipToLookup += recip

                if (type == 1) {
                    DatabaseUtils.dumpCurrentRow(it)
                }

                msg += MessagePartial(id.toString(), recip, message, date, type)
                it.moveToNext()
                i++
            }
        }

        val recipAddrMap = mutableMapOf<String, String>()
        val recipContactMap = mutableMapOf<String, String>()
        recipToLookup.forEach { recip ->
            // recipient_id -> canonical-address -> contact lookup

            contentResolver.query(
                Uri.parse("content://mms-sms/canonical-addresses"),
                null,
                "_id=${recip}",
                null, null
            )?.use {
                it.moveToFirst()
                val addr = it.getString(it.getColumnIndexOrThrow("address"))
                recipAddrMap[recip] = addr
            }
        }

        val threadMessageIdMap = mutableMapOf<String, String>()
        msg.forEach { msg ->
            if (msg.type == 1) {
                contentResolver.query(Uri.parse("content://mms"), null, "thread_id=${msg.id}", null, "date DESC")?.use {
                    it.moveToFirst()
                    if (!it.isAfterLast) {
                        val first = it.getString(it.getColumnIndexOrThrow("_id")) // or m_id ?
                        threadMessageIdMap[msg.id] = first
                    }

                    // it.moveToFirst()
                    // while (!it.isAfterLast)  {
                    //     DatabaseUtils.dumpCurrentRow(it)
                    //     val partId: String = it.getString(it.getColumnIndexOrThrow("_id"))
                    //     val type: String = it.getString(it.getColumnIndexOrThrow("ct"))
                    //     if ("text/plain" == type) {
                    //         val data: String? = it.getStringOrNull(it.getColumnIndexOrThrow("_data"))
                    //         var body: String?
                    //         if (data != null) {
                    //             body = getMmsText(partId)
                    //         } else {
                    //             body = it.getString(it.getColumnIndexOrThrow("text"))
                    //         }
                    //         println("Body is $body")
                    //     }
                    //     it.moveToNext()
                    // }
                }
            }
        }

        val threadIdBodyMap = mutableMapOf<String, String>()
        threadMessageIdMap.forEach { threadId, messageId ->
            val selectionPart = "mid=$messageId"
            val uri = Uri.parse("content://mms/part")
            contentResolver.query(
                uri, null,
                selectionPart, null, null
            )?.use {

                if (it.moveToFirst()) {
                    do {
                        val partId = it!!.getString(it!!.getColumnIndexOrThrow("_id"))
                        val type = it!!.getString(it!!.getColumnIndexOrThrow("ct"))
                        if ("text/plain" == type) {
                            val data = it!!.getString(it!!.getColumnIndexOrThrow("_data"))
                            var body: String = if (data != null) {
                                // implementation of this method below
                                getMmsText(partId)
                            } else {
                                it.getString(it.getColumnIndexOrThrow("text"))
                            }
                            threadIdBodyMap[threadId] = body
                        }
                    } while (it.moveToNext())
                }
            }
        }

        recipAddrMap.forEach { recip, addr ->
            contentResolver.query(
                Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, addr),
                null,
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

        smsState.value = msg.map {
            MessageThread(
                id = it.id,
                participants = it.recipients.map { recipId -> recipContactMap[recipId] ?: recipAddrMap[recipId] ?: "" },
                message = if (it.type == 0) it.message else threadIdBodyMap[it.id] ?: "Empy MSS",
                time = it.date
            )
        }
    }

    private fun getMmsText(id: String): String {
        val partURI = Uri.parse("content://mms/part/$id")
        var `is`: InputStream? = null
        val sb = StringBuilder()
        try {
            `is` = contentResolver.openInputStream(partURI)
            if (`is` != null) {
                val isr = InputStreamReader(`is`, "UTF-8")
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
            if (`is` != null) {
                try {
                    `is`.close()
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

data class MessageThread(
    val id: String,
    val participants: List<String>,
    val message: String,
    val time: Long
)