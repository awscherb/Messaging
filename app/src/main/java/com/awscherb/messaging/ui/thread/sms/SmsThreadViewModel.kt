package com.awscherb.messaging.ui.thread.sms

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.awscherb.messaging.pager.SmsPager
import com.awscherb.messaging.ui.thread.common.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class SmsThreadViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext context: Context
) : AndroidViewModel(context as Application) {
    val threadId = savedStateHandle.get<String>("id")

    val messages = MutableStateFlow<List<Message>>(emptyList())

    val pagingFlow: Flow<PagingData<Message>> = SmsPager(context).getPagerFlow(threadId!!)

    init {
        val messagesList = mutableListOf<Message>()
        context.contentResolver.query(
            Uri.parse("content://sms"),
            null,
            "thread_id=$threadId",
            null, "DATE DESC LIMIT 10"
        )?.use {
            it.moveToFirst()
            while (!it.isAfterLast) {
                val message = it.getString(it.getColumnIndexOrThrow("body"))
                val id = it.getString(it.getColumnIndexOrThrow("_id"))
                val fromMe = it.getInt(it.getColumnIndexOrThrow("type")) == 2
                messagesList += Message(id, message, fromMe, null)
                it.moveToNext()
            }
        }

        messages.value = messagesList
    }
}

