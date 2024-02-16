package com.awscherb.messaging.ui.base

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.awscherb.messaging.data.MessageType
import com.awscherb.messaging.ui.messages.MessagesScreen
import com.awscherb.messaging.ui.theme.MessagingTheme
import com.awscherb.messaging.ui.thread.mms.MmsThreadScreen
import com.awscherb.messaging.ui.thread.sms.SmsThreadScreen
import com.awscherb.messaging.worker.ThreadImportWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class MainActivity : ComponentActivity() {


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
                            MessagesScreen {
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
                            SmsThreadScreen {
                                navController.popBackStack()
                            }
                        }
                        composable(Destination.MmsThread.dest, arguments = listOf(
                            navArgument(name = "id") {
                                type = NavType.StringType
                            }
                        )) {
                            MmsThreadScreen {
                                navController.popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startImport() {
        val req = OneTimeWorkRequestBuilder<ThreadImportWorker>()
            .addTag(ThreadImportWorker.ThreadsFullSync)
            .build()

        with(WorkManager.getInstance(this)) {
            enqueue(req)

            getWorkInfoByIdFlow(req.id)
                .onEach {
                    it.progress.getString(ThreadImportWorker.Step)
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

