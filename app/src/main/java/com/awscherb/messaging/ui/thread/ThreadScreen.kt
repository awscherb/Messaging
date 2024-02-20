package com.awscherb.messaging.ui.thread

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.awscherb.messaging.data.Message
import com.awscherb.messaging.data.MessageThread
import com.awscherb.messaging.data.MessageType
import com.awscherb.messaging.ui.base.ScaffoldScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun ThreadScreen(
    viewModel: ThreadScreenViewModel = hiltViewModel(),
    onBackPressed: () -> Unit
) {

    val thread by viewModel.thread.collectAsState(initial = null)

    ThreadScreenInner(
        thread = thread,
        messages = viewModel.pagingFlow,
        onBackPressed = onBackPressed,
        sendMessage = { viewModel.sendTextMessage(it) })
}


@Composable
fun ThreadScreenInner(
    thread: MessageThread?,
    messages: Flow<PagingData<Message>>,
    onBackPressed: () -> Unit,
    sendMessage: (String) -> Unit
) {
    var inputText by remember {
        mutableStateOf("")
    }
    ScaffoldScreen(
        title = thread?.getTitle() ?: "Message",
        navIcon = Icons.AutoMirrored.Default.ArrowBack,
        bottomBar = {
            Row {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(text = "Message")
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    trailingIcon = {
                        Icon(Icons.AutoMirrored.Default.Send, contentDescription = "Send",
                            modifier = if (inputText.isEmpty()) Modifier else Modifier.clickable {
                                sendMessage(inputText)
                                inputText = ""
                            })
                    }
                )

            }
        },
        navOnClick = { onBackPressed() }) {

        val lazyPagingItems = messages.collectAsLazyPagingItems()

        LazyColumn(
            modifier = Modifier.padding(it),
            reverseLayout = true
        ) {
            items(
                count = lazyPagingItems.itemCount,
                key = lazyPagingItems.itemKey { it.id },
            ) {
                val message = lazyPagingItems[it] ?: return@items
                MessageBubble(message = message)
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun ThreadScreenPreview() {
    ThreadScreenInner(
        thread = MessageThread(
            "!", "Last message", 0L, emptyList(),
            emptyList(),
            false, false, MessageType.SMS,
        ),
        messages = flowOf(),
        sendMessage = {},
        onBackPressed = {})
}
