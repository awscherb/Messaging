package com.awscherb.messaging.ui.thread.sms

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.awscherb.messaging.ui.thread.common.Message
import com.awscherb.messaging.ui.base.ScaffoldScreen
import com.awscherb.messaging.ui.thread.common.MessageList
import com.awscherb.messaging.ui.thread.common.ThreadScreenInner

@Composable
fun SmsThreadScreen(
    viewModel: SmsThreadViewModel = hiltViewModel(),
    onBackPressed: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    ThreadScreenInner(messages, onBackPressed)
}


