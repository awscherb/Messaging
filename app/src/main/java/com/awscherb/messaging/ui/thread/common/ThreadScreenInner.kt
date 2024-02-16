package com.awscherb.messaging.ui.thread.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.awscherb.messaging.ui.base.ScaffoldScreen
import kotlinx.coroutines.flow.Flow

@Composable
fun ThreadScreenInner(
    messages: List<Message>,
    onBackPressed: () -> Unit
) {
    ScaffoldScreen(
        title = "Messages",
        navIcon = Icons.AutoMirrored.Default.ArrowBack,
        navOnClick = { onBackPressed() }) {

        MessageList(modifier = Modifier.padding(it), messages = messages)
    }
}

@Composable
fun ThreadScreenInnerPaging(
    messages: Flow<PagingData<Message>>,
    onBackPressed: () -> Unit
) {
    ScaffoldScreen(
        title = "Messages",
        navIcon = Icons.AutoMirrored.Default.ArrowBack,
        navOnClick = { onBackPressed() }) {

        val lazyPagingItems = messages.collectAsLazyPagingItems()

        LazyColumn(modifier = Modifier.padding(it), reverseLayout = true) {
            items(
                count = lazyPagingItems.itemCount,
                key = lazyPagingItems.itemKey { it.id },
            ) {
                val message = lazyPagingItems[it] ?: return@items
                if (message.fromMe) MessageFromMe(message = message) else MessageToMe(message = message)
            }
        }
    }
}

@Composable
@Preview
fun ThreadsPreview() {
    ThreadScreenInner(
        listOf(
            Message("1", "Hey", true, null),
            Message("1", "Hey", false, null),
        )
    ) {

    }
}