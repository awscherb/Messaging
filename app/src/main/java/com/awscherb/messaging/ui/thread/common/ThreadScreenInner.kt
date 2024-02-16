package com.awscherb.messaging.ui.thread.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.awscherb.messaging.ui.base.ScaffoldScreen

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