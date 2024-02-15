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
import com.awscherb.messaging.MessageThread
import com.awscherb.messaging.ui.theme.MessagingTheme

@Composable
fun MessagesScreen(
    messages: List<MessageThread>,
    onLoadMore : () -> Unit
) {
    LazyColumn {
        items(messages) {
            MessageRow(messageThread = it)
        }
        item {
            Text(text = "Load More",
                modifier = Modifier.padding(all = 16.dp).clickable { onLoadMore() })
        }
    }
}

@Composable
@Preview(apiLevel = 33)
fun MessageScreenPreview() {
    MessagingTheme {
        MessagesScreen(
            messages =
            listOf(MessageThread("1", listOf("212"), "message", System.currentTimeMillis()))
        ) {

        }
    }
}