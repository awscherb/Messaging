package com.awscherb.messaging.ui.base

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.awscherb.messaging.data.MessageType
import com.awscherb.messaging.service.ContactService
import com.awscherb.messaging.ui.threadList.ThreadListScreen
import com.awscherb.messaging.ui.theme.MessagingTheme
import com.awscherb.messaging.ui.theme.PurpleGrey40
import com.awscherb.messaging.ui.thread.ThreadScreen
import com.awscherb.messaging.worker.ThreadImportWorker
import com.awscherb.messaging.worker.ThreadRecordWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var contactService: ContactService


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

    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        if (Permissions.any {
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }) {
            request.launch(Permissions)
        } else {
            startImport()
        }

        val startDest = Destination.Messages
        setContent {
            MessagingTheme {
                navController = rememberNavController()
                var selectedItem by remember {
                    mutableStateOf<Destination>(startDest)
                }
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                ModalNavigationDrawer(drawerState = drawerState,
                    drawerContent = { /*TODO*/ })
                {
                    NavHost(
                        navController = navController,
                        startDestination = startDest.dest
                    ) {
                        composable(Destination.Messages.dest) {
                            ThreadListScreen {
                                syncRecords(it.threadId)
                                when (it.threadType) {
                                    MessageType.SMS -> navController.navigate("sms/${it.threadId}")
                                    MessageType.MMS -> navController.navigate("mms/${it.threadId}")
                                }
                            }
                        }

                        composable(Destination.SmsThread.dest, arguments = listOf(
                            navArgument(name = "id") {
                                type = NavType.StringType
                            }
                        )) {
                            ThreadScreen {
                                navController.popBackStack()
                            }
                        }
                        composable(Destination.MmsThread.dest, arguments = listOf(
                            navArgument(name = "id") {
                                type = NavType.StringType
                            }
                        )) {
                            ThreadScreen {
                                navController.popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun syncRecords(threadId: String) {
        val req = OneTimeWorkRequestBuilder<ThreadRecordWorker>()
            .setInputData(workDataOf(ThreadRecordWorker.THREAD_ID to arrayOf(threadId)))
            .build()

        WorkManager.getInstance(this)
            .enqueue(req)
    }

    private fun startImport() {
        val req = OneTimeWorkRequestBuilder<ThreadImportWorker>()
            .addTag(ThreadImportWorker.ThreadsFullSync)
            .build()

        with(WorkManager.getInstance(this)) {
            enqueue(req)

            getWorkInfoByIdFlow(req.id)
                .onEach {
                    when (it?.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            val topThreads = it.outputData.getStringArray(
                                ThreadImportWorker.TOP_THREADS
                            ) ?: emptyArray()

                            val syncMessages = OneTimeWorkRequestBuilder<ThreadRecordWorker>()
                                .setInputData(workDataOf(
                                    ThreadRecordWorker.THREAD_ID to topThreads
                                ))
                                .build()

                            enqueue(syncMessages)

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

