package com.awscherb.messaging

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.database.DatabaseUtils
import android.os.Build
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.Telephony.TextBasedSmsColumns
import android.provider.Telephony.Threads
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.awscherb.messaging.ui.messages.MessagesScreen
import java.util.Date


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_SMS),
                6
            )
        } else {

            setContent {

                MessagesScreen(
                    messages = readSms()
                )
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        readSms()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun readSms(): MutableList<MessageThread> {
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
        ).use {
            it ?: return mutableListOf()

            it.moveToFirst()
            var i = 0
            while (!it.isAfterLast && i < 10) {
                ids += MessageThread(
                    id = it.getString(it.getColumnIndexOrThrow(BaseColumns._ID)) ?: "",
                    from = it.getString(it.getColumnIndexOrThrow(TextBasedSmsColumns.ADDRESS)),
                    message = it.getString(it.getColumnIndexOrThrow(TextBasedSmsColumns.BODY)),
                    time = it.getLong(it.getColumnIndexOrThrow(TextBasedSmsColumns.DATE_SENT))

                )
                it.moveToNext()
                i++

            }
        }

        return ids

    }

}

data class MessageThread(
    val id: String,
    val from: String,
    val message: String,
    val time: Long
)