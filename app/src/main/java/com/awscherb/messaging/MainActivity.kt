package com.awscherb.messaging

import android.Manifest
import android.content.pm.PackageManager
import android.database.DatabaseUtils
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Telephony
import android.provider.Telephony.Mms
import android.provider.Telephony.Sms.Conversations
import android.provider.Telephony.Sms.Intents
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.database.getStringOrNull
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.awscherb.messaging.ui.messages.MessagesScreen
import com.awscherb.messaging.worker.ThreadImportWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dao: ThreadDao


    companion object {
        private val Permissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS
        )
    }

    private val request =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it.all { (_, v) -> v }) {
                startImport()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        if (Permissions.any {
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }) {
            request.launch(Permissions)
        } else {
            startImport()
        }

        setContent {
            val threads by dao.listAllThreads().collectAsState(initial = emptyList())
            println("Thread size is ${threads.size}")
            MessagesScreen(
                messages = threads
            ) {
            }
        }
    }

    private fun startImport() {
        val req = OneTimeWorkRequestBuilder<ThreadImportWorker>()
            .build()

        with(WorkManager.getInstance(this)) {
            enqueue(req)

            getWorkInfoByIdFlow(req.id)
                .onEach {
                    when (it.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            println("Import SUCCESS")
                        }

                        WorkInfo.State.FAILED -> {
                            println("Import FAILED")
                        }

                        else -> {
                        }
                    }
                }.launchIn(lifecycleScope)
        }
    }

}

