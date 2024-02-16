package com.awscherb.messaging.ui.thread.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.awscherb.messaging.ui.theme.Pink80
import com.awscherb.messaging.ui.theme.Purple80

@Composable
fun MessageList(
    modifier: Modifier = Modifier,
    messages: List<Message>
) {
    LazyColumn(modifier = modifier, reverseLayout = true) {
        items(messages, key = { it.id }, contentType = { it.fromMe }) {
            if (it.fromMe) MessageFromMe(message = it) else MessageToMe(message = it)
        }
    }
}

@Composable
fun MessageFromMe(
    message: Message
) {
    Column(
        modifier = Modifier
            .padding(
                horizontal = 16.dp,
                vertical = 2.dp
            )
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Pink80)
                .align(Alignment.End)
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                text = message.text,
            )
        }
        message.contact?.let {
            Text(
                text = message.contact,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun MessageToMe(
    message: Message
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Purple80)
                .align(Alignment.Start)
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                text = message.text,
            )
        }
        message.contact?.let {
            Text(
                text = message.contact,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}


@Preview(showSystemUi = true)
@Composable
fun MessageListPreview() {
    MessageList(
        messages = listOf(
            Message("1", "Hello, world!", true, "3125550690"),
            Message("2", "hey back!", false, "Contact"),
        )
    )
}