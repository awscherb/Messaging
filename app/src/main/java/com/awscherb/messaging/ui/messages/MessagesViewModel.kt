package com.awscherb.messaging.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.awscherb.messaging.ThreadDao
import com.awscherb.messaging.worker.ThreadImportWorker
import com.awscherb.messaging.worker.ThreadImportWorker.Companion.ThreadsFullSync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val threadsDao: ThreadDao,
    private val workManager: WorkManager
) : ViewModel() {

    val progress = MutableStateFlow<String?>(null)

    val messages = threadsDao.listAllThreads()

    init {

        workManager.getWorkInfosByTagFlow(ThreadsFullSync)
            .filter { it.isNotEmpty() }
            .map { it.first() }
            .onEach { work ->
                if (work.state == WorkInfo.State.FAILED || work.state == WorkInfo.State.SUCCEEDED) {
                    progress.value = null
                } else {
                    work.progress.getString(ThreadImportWorker.Step)?.let { step ->
                        progress.value = step
                    }
                }

            }.launchIn(viewModelScope)

    }

}