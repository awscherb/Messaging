package com.awscherb.messaging.ui.thread.mms


import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.awscherb.messaging.ui.thread.common.ThreadScreenInner

@Composable
fun MmsThreadScreen(
    viewModel: MmsThreadViewModel = hiltViewModel(),
    onBackPressed: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    ThreadScreenInner(messages, onBackPressed)
}
