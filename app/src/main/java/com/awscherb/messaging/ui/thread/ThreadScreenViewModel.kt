package com.awscherb.messaging.ui.thread

import android.app.Application
import android.content.Context
import android.telephony.SmsManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.awscherb.messaging.dao.ThreadDao
import com.awscherb.messaging.content.MmsHelper
import com.awscherb.messaging.dao.ThreadMessageRecordDao
import com.awscherb.messaging.data.Message
import com.awscherb.messaging.data.MessageThread
import com.awscherb.messaging.data.MessageType
import com.awscherb.messaging.pager.SmsMmsPager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ThreadScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    mmsHelper: MmsHelper,
    threadDao: ThreadDao,
    threadMessageRecordDao: ThreadMessageRecordDao,
    @ApplicationContext context: Context
) : AndroidViewModel(context as Application) {

    private val smsService = context.getSystemService(SmsManager::class.java)

    val threadId = savedStateHandle.get<String>("id")!!

    val thread: StateFlow<MessageThread?> =
        threadDao.getThread(threadId).filter { it.isNotEmpty() }.map { it[0] }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val pagingFlow: Flow<PagingData<Message>> =
        SmsMmsPager(context, threadMessageRecordDao, mmsHelper).getPagerFlow(threadId)

    fun sendTextMessage(message: String) {
        thread.value?.let {
            when (it.threadType) {
                MessageType.SMS -> {
                    smsService.sendTextMessage(it.addresses.first(), null, message, null, null)
                }
                MessageType.MMS -> {
                    smsService.sendTextMessage(it.addresses.first(), null, message, null, null)
                }
            }
        }
    }

    init {
        println("thread id $threadId")
    }
}

