package com.awscherb.messaging.ui.thread

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.awscherb.messaging.content.MmsHelper
import com.awscherb.messaging.pager.SmsMmsPager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class ThreadScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mmsHelper: MmsHelper,
    @ApplicationContext context: Context
) : AndroidViewModel(context as Application) {
    val threadId = savedStateHandle.get<String>("id")

    val messages = MutableStateFlow<List<Message>>(emptyList())

    val pagingFlow: Flow<PagingData<Message>> = SmsMmsPager(context, mmsHelper).getPagerFlow(threadId!!)

    fun sendMessage(message: String) {

    }

    init {
        println("thread id $threadId")


    }
}

