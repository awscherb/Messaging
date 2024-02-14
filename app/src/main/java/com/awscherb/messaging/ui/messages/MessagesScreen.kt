package com.awscherb.messaging.ui.messages

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import com.awscherb.messaging.MessageThread

@Composable
fun MessagesScreen(
    messages: List<MessageThread>
) {
    LazyColumn {
        items(messages) {
            MessageRow(messageThread = it)
        }
    }

}