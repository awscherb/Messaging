package com.awscherb.messaging.ui.threadList

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.awscherb.messaging.data.MessageThread
import com.awscherb.messaging.data.MessageType
import com.awscherb.messaging.ui.theme.MessagingTheme
import com.awscherb.messaging.ui.theme.Typography
import com.awscherb.messaging.ui.util.TimeHolder
import java.util.Date

@Composable
fun ThreadRow(
    messageThread: MessageThread,
    onClick: (MessageThread) -> Unit = {}
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick(messageThread)
            }
            .padding(top = 8.dp, bottom = 8.dp)
    ) {

        Box(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {


            val names = getPreviewText(messageThread.participants)

            when (names.size) {
                0 -> throw IllegalArgumentException("Need at least one name")
                1 -> MessageNameCircle(
                    name = names[0],
                    size = 48.dp,
                    fontSize = TextUnit.Unspecified
                )

                2 -> Row {
                    MessageNameCircle(
                        name = names[0],
                        size = 24.dp,
                        fontSize = Typography.bodySmall.fontSize
                    )
                    MessageNameCircle(
                        name = names[1],
                        size = 24.dp,
                        fontSize = Typography.bodySmall.fontSize
                    )
                }

                3 -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row {
                        MessageNameCircle(
                            name = names[0],
                            size = 24.dp,
                            fontSize = Typography.bodySmall.fontSize
                        )
                        MessageNameCircle(
                            name = names[1],
                            size = 24.dp,
                            fontSize = Typography.bodySmall.fontSize
                        )
                    }
                    MessageNameCircle(
                        name = names[2],
                        size = 24.dp,
                        fontSize = Typography.bodySmall.fontSize
                    )
                }

                else ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row {
                            MessageNameCircle(
                                name = names[0],
                                size = 24.dp,
                                fontSize = Typography.bodySmall.fontSize
                            )
                            MessageNameCircle(
                                name = names[1],
                                size = 24.dp,
                                fontSize = Typography.bodySmall.fontSize
                            )
                        }
                        Row {
                            val lastName = if (names.size == 4) names[3] else "+${names.size - 3}"
                            MessageNameCircle(
                                name = names[2],
                                size = 24.dp,
                                fontSize = Typography.bodySmall.fontSize
                            )
                            MessageNameCircle(
                                name = lastName,
                                size = 24.dp,
                                fontSize = Typography.bodySmall.fontSize
                            )
                        }
                    }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)

        ) {

            Text(
                text = messageThread.getTitle(),
                modifier = Modifier,
                style = Typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val prefix = if (messageThread.fromMe) "You: " else ""
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = "$prefix${messageThread.message}",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (!messageThread.read) FontWeight.Bold else null
            )
        }

        Text(
            modifier = Modifier
                .padding(
                    top = 4.dp,
                    end = 16.dp,
                ),
            text = TimeHolder.prettyTime.format(Date(messageThread.date)),
            maxLines = 1,


            )
    }
}

@Composable
fun MessageNameCircle(
    name: String,
    size: Dp,
    fontSize: TextUnit
) {
    Box(
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
        )
        Text(
            text = name,
            fontSize = fontSize
        )
    }
}

private fun getPreviewText(names: List<String>): List<String> {
    return if (names.isEmpty() || names.all { it.isBlank() }) {
        listOf("?")
    } else if (names.all { it.startsWith("+") }) {
        listOf("#")
    } else {
        names.map { it.toInitials() }
    }
}

private fun String.toInitials(): String {
    return when {
        this.isEmpty() -> ""
        !this.contains(" ") -> this[0].toString()
        else -> {
            val parts = this.split(" ")
            return parts[0][0].toString() + parts[1][0].toString()
        }
    }
}


@Preview(apiLevel = 33, showBackground = true)
@Composable
fun MessageRowPreview() {
    MessagingTheme {
        ThreadRow(
            messageThread =
            MessageThread(
                threadId = "1",
                participants = listOf("First Name"),
                message = "Hello, world",
                date = System.currentTimeMillis() - 1232222,
                fromMe = false,
                read = true,
                threadType = MessageType.SMS,
                addresses = emptyList()
            )
        )
    }
}

@Preview(apiLevel = 33, showBackground = true)
@Composable
fun MessageRowEmptyNamePreview() {
    MessagingTheme {
        ThreadRow(
            messageThread = MessageThread(
                threadId = "1",
                participants = emptyList(),
                message = "Hello, world with a super long message and some will be cut off!",
                date = System.currentTimeMillis() - 5000000000,
                fromMe = true,
                read = true,
                threadType = MessageType.SMS,
                addresses = emptyList()
            )
        )
    }
}

@Preview(apiLevel = 33, showBackground = true)
@Composable
fun MessageRowNumberPreview() {
    MessagingTheme {
        ThreadRow(
            messageThread =
            MessageThread(
                threadId = "1",
                participants = listOf("+13125550690"),
                message = "Hello, world with a super long message and some will be cut off!",
                date = System.currentTimeMillis(),
                read = false,
                fromMe = false,
                threadType = MessageType.SMS,
                addresses = emptyList()
            )
        )
    }
}

@Preview(apiLevel = 33, showBackground = true)
@Composable
fun MessageRowEmptyNameStringPreview() {
    MessagingTheme {
        ThreadRow(
            messageThread =
            MessageThread(
                threadId = "1",
                participants = listOf(""),
                message = "Hello, world with a super long message and some will be cut off!",
                date = System.currentTimeMillis(),
                fromMe = true,
                read = false,
                threadType = MessageType.SMS,
                addresses = emptyList()
            )
        )
    }
}

@Preview(apiLevel = 33, showBackground = true)
@Composable
fun MessageRowTwoNamePreview() {
    MessagingTheme {
        ThreadRow(
            messageThread =
            MessageThread(
                threadId = "1",
                participants = listOf("First Name", "Second Name"),
                message = "Hello, world with a super long message and some will be cut off!",
                date = System.currentTimeMillis() - 50000000000,
                read = true,
                fromMe = true,
                threadType = MessageType.SMS,
                addresses = emptyList()
            )
        )
    }
}

@Preview(apiLevel = 33, showBackground = true)
@Composable
fun MessageRowThreeNamePreview() {
    MessagingTheme {
        ThreadRow(
            messageThread =
            MessageThread(
                threadId = "1",
                participants = listOf("First Name", "SecondName", "Third Name"),
                message = "Hello, world with a super long message and some will be cut off!",
                date = System.currentTimeMillis(),
                read = true,
                fromMe = false,
                threadType = MessageType.SMS,
                addresses = emptyList()
            )
        )
    }
}

@Preview(apiLevel = 33, showBackground = true)
@Composable
fun MessageRowFourNamePreview() {
    MessagingTheme {
        ThreadRow(
            messageThread =
            MessageThread(
                threadId = "1",
                participants = listOf("First Name", "Second Name", "Third Name", "Fourth Name"),
                message = "Hello, world with a super long message and some will be cut off!",
                date = System.currentTimeMillis(),
                fromMe = true,
                read = false,
                threadType = MessageType.SMS,
                addresses = emptyList()
            )
        )
    }
}

@Preview(apiLevel = 33, showBackground = true)
@Composable
fun MessageRowManyPreview() {
    MessagingTheme {
        ThreadRow(
            messageThread =
            MessageThread(
                threadId = "1",
                participants = listOf(
                    "FirstName",
                    "Second Name",
                    "Third Name",
                    "Fourth Name",
                    "Fifth Name",
                    "Sixth Name"
                ),
                message = "Hello, world with a super long message and some will be cut off!",
                date = System.currentTimeMillis(),
                read = false,
                fromMe = false,
                threadType = MessageType.SMS,
                addresses = emptyList()
            )
        )
    }
}