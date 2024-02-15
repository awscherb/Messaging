package com.awscherb.messaging

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.ContactsContract
import android.provider.Telephony.TextBasedSmsColumns
import android.provider.Telephony.Threads
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.database.getStringOrNull
import androidx.lifecycle.coroutineScope
import com.awscherb.messaging.ui.messages.MessagesScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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
                val recip = it.getString(it.getColumnIndexOrThrow("recipient_ids")).split(" ")
                val date = it.getLong(it.getColumnIndexOrThrow("date"))
                recipToLookup += recip
                msg += MessagePartial(id.toString(), recip, message, date)
                it.moveToNext()
                i++
            }
            println("simple count is ${it.count}")
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
            MessageThread(id = it.id,
                participants = it.recipients.map { recipId -> recipContactMap[recipId] ?: recipAddrMap[recipId] ?: "" },
                message = it.message,
                time = it.date)
        }

    }

}

data class MessagePartial(
    val id: String,
    val recipients: List<String>,
    val message: String,
    val date: Long
)

data class MessageThread(
    val id: String,
    val participants: List<String>,
    val message: String,
    val time: Long
)