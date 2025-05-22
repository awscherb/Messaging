@file:OptIn(ExperimentalMaterial3Api::class)

package com.awscherb.messaging.ui.threadList

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.awscherb.messaging.data.MessageThread
import com.awscherb.messaging.data.MessageType
import com.awscherb.messaging.ui.base.ScaffoldScreen
import com.awscherb.messaging.ui.theme.MessagingTheme

/**
 * List of threads, your inbox
 */
@Composable
fun ThreadListScreen(
    viewModel: ThreadsViewModel = hiltViewModel(),
    threadOnClick: (MessageThread) -> Unit
) {
    val progress by viewModel.progress.collectAsState()
    val messages by viewModel.messages.collectAsState(initial = emptyList())
    ThreadListScreenInner(
        progress = progress,
        messages = messages,
        threadOnClick = threadOnClick
    )
}

@Composable
fun ThreadListScreenInner(
    progress: String? = null,
    messages: List<MessageThread>,
    threadOnClick: (MessageThread) -> Unit
) {
    ScaffoldScreen(title = "Messages", navOnClick = { }) {
        if (progress != null) {
            Column(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Preparing data for the first time",
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                CircularProgressIndicator()
                Text(
                    text = progress,
                    modifier = Modifier.padding(top = 16.dp)
                )

            }

        } else {
            LazyColumn(modifier = Modifier.padding(it)) {
                items(messages, contentType = { 1 }, key = { it.threadId }) {
                    ThreadRow(messageThread = it, threadOnClick)
                }
            }
        }
    }
}

@Composable
@Preview(apiLevel = 33)
fun MessageScreenPreview() {
    MessagingTheme {
        ThreadListScreenInner(
            messages = listOf(
                MessageThread(
                    threadId = "1",
                    message = "message",
                    date = System.currentTimeMillis(),
                    participants = listOf("212"),
                    read = false,
                    fromMe = false,
                    threadType = MessageType.SMS,
                    addresses = emptyList()
                )
            )
        ) {

        }
    }
}

@Composable
@Preview(apiLevel = 33)
fun MessageScreenProgressPreview() {
    MessagingTheme {
        ThreadListScreenInner(
            progress = "Fetching threads",
            messages = listOf()
        ) {

        }
    }
}