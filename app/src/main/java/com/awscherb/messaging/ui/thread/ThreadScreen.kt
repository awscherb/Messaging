package com.awscherb.messaging.ui.thread

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.awscherb.messaging.ui.base.ScaffoldScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun ThreadScreen(
    viewModel: ThreadScreenViewModel = hiltViewModel(),
    onBackPressed: () -> Unit
) {
    ThreadScreenInner(viewModel.pagingFlow, onBackPressed,
        sendMessage = { viewModel.sendMessage(it) })
}


@Composable
fun ThreadScreenInner(
    messages: Flow<PagingData<Message>>,
    onBackPressed: () -> Unit,
    sendMessage: (String) -> Unit
) {
    var inputText by remember {
        mutableStateOf("")
    }
    ScaffoldScreen(
        title = "Messages",
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
                    trailingIcon = {
                        Icon(Icons.AutoMirrored.Default.Send, contentDescription = "Send",
                            modifier = Modifier.clickable {
                                sendMessage(inputText)
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
    ThreadScreenInner(messages = flowOf(), {}) {

    }
}
