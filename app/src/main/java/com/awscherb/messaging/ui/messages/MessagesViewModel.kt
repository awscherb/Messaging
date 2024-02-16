package com.awscherb.messaging.ui.messages

import androidx.lifecycle.ViewModel
import com.awscherb.messaging.ThreadDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val threadsDao: ThreadDao
) : ViewModel() {

}