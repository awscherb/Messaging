package com.awscherb.messaging

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.database.DatabaseUtils
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.ContactsContract
import android.provider.Telephony
import android.provider.Telephony.TextBasedSmsColumns
import android.provider.Telephony.Threads
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.awscherb.messaging.ui.messages.MessagesScreen
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Date

class MainActivity : ComponentActivity() {

    private val smsState = MutableStateFlow<List<MessageThread>>(emptyList())

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
            readSms()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Permissions.any {
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }) {
            request.launch(Permissions)
        } else {
            readSms()
        }
        setContent {
            val messages by smsState.collectAsState()
            MessagesScreen(
                messages = messages
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun readSms() {
        val ids = mutableListOf<MessageThread>()
        contentResolver.query(
            Threads.CONTENT_URI,
            null,
            Bundle().apply {
                putString(
                    ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                    "${TextBasedSmsColumns.DATE} DESC"
                )
            },
            null,
        )?.use {
            it.moveToFirst()
            var i = 0
            while (!it.isAfterLast && i < 10) {

                val from = it.getString(it.getColumnIndexOrThrow(TextBasedSmsColumns.ADDRESS))
                val name = contentResolver.query(
                    Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, from),
                    null,
                    null,
                    null,
                    null
                )?.use {
                    it.moveToFirst()
                    if (!it.isAfterLast) {
                        DatabaseUtils.dumpCurrentRow(it)
                        val gotten = it.getString(it.getColumnIndexOrThrow("display_name"))
                        println("WE GOT $gotten")
                        gotten
                    } else from
                } ?: from


                ids += MessageThread(
                    id = it.getString(it.getColumnIndexOrThrow(BaseColumns._ID)) ?: "",
                    from = name,
                    message = it.getString(it.getColumnIndexOrThrow(TextBasedSmsColumns.BODY)),
                    time = it.getLong(it.getColumnIndexOrThrow(TextBasedSmsColumns.DATE_SENT))

                )
                it.moveToNext()
                i++
            }
        }


        smsState.value = ids
    }
}

data class MessageThread(
    val id: String,
    val from: String,
    val message: String,
    val time: Long
)