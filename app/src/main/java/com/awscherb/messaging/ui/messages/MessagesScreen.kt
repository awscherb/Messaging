package com.awscherb.messaging.ui.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.awscherb.messaging.data.MessageThread
import com.awscherb.messaging.ui.ScaffoldScreen
import com.awscherb.messaging.ui.theme.MessagingTheme

@Composable
fun MessagesScreen(
    messages: List<MessageThread>,
    onLoadMore: () -> Unit
) {
    ScaffoldScreen(title = "Messages", navOnClick = { /*TODO*/ }) {

        LazyColumn(modifier = Modifier.padding(it)) {
            items(messages, contentType = { 1 }, key = { it.threadId }) {
                MessageRow(messageThread = it)
            }
        }
    }
}

@Composable
@Preview(apiLevel = 33)
fun MessageScreenPreview() {
    MessagingTheme {
        MessagesScreen(
            messages =
            listOf(
                MessageThread(
                    threadId = "1",
                    message = "message",
                    date = System.currentTimeMillis(),
                    participants = listOf("212")
                )
            )
        ) {

        }
    }
}