package com.awscherb.messaging.ui.thread.sms

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.awscherb.messaging.ui.thread.common.ThreadScreenInnerPaging

@Composable
fun SmsThreadScreen(
    viewModel: SmsThreadViewModel = hiltViewModel(),
    onBackPressed: () -> Unit
) {
    ThreadScreenInnerPaging(viewModel.pagingFlow, onBackPressed)
}


