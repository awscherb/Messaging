package com.awscherb.messaging.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.awscherb.messaging.MessageThread
import com.awscherb.messaging.ui.theme.MessagingTheme
import com.awscherb.messaging.ui.theme.Purple80
import com.awscherb.messaging.ui.theme.Typography
import org.ocpsoft.prettytime.PrettyTime
import java.util.Date

@Composable
fun MessageRow(
    messageThread: MessageThread
) {

    ConstraintLayout(
        modifier = Modifier.fillMaxWidth()
    ) {
        val (who, message, time) = createRefs()

        Box(
            modifier = Modifier
                .constrainAs(who) {
                    linkTo(
                        top = parent.top,
                        bottom = parent.bottom,
                        end = message.start,
                        start = parent.start
                    )
                }
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
                            val lastName = if (names.size == 4) names[3] else "..."
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

        Text(
            modifier = Modifier.constrainAs(message) {
                linkTo(
                    top = parent.top,
                    bottom = parent.bottom,
                    start = who.end,
                    end = time.start
                )
                width = Dimension.fillToConstraints
            },
            text = messageThread.message,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            modifier = Modifier
                .constrainAs(time) {
                    top.linkTo(message.top)
                    end.linkTo(parent.end)

                }
                .padding(
                    end = 16.dp,
                ),
            text = PrettyTime().format(Date(messageThread.time)),
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
                .background(Purple80)
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
        !this.contains(" ") -> this[0].toString().capitalize()
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
        MessageRow(
            messageThread =
            MessageThread(
                id = "1",
                participants = listOf("First Name"),
                message = "Hello, world with a super long message and some will be cut off!",
                time = System.currentTimeMillis()
            )
        )
    }
}

@Preview(apiLevel = 33, showBackground = true)
@Composable
fun MessageRowEmptyNamePreview() {
    MessagingTheme {
        MessageRow(
            messageThread =
            MessageThread(
                id = "1",
                participants = emptyList(),
                message = "Hello, world with a super long message and some will be cut off!",
                time = System.currentTimeMillis()
            )
        )
    }
}

@Preview(apiLevel = 33, showBackground = true)
@Composable
fun MessageRowNumberPreview() {
    MessagingTheme {
        MessageRow(
            messageThread =
            MessageThread(
                id = "1",
                participants = listOf("+13125550690"),
                message = "Hello, world with a super long message and some will be cut off!",
                time = System.currentTimeMillis()
            )
        )
    }
}

@Preview(apiLevel = 33, showBackground = true)
@Composable
fun MessageRowEmptyNameStringPreview() {
    MessagingTheme {
        MessageRow(
            messageThread =
            MessageThread(
                id = "1",
                participants = listOf(""),
                message = "Hello, world with a super long message and some will be cut off!",
                time = System.currentTimeMillis()
            )
        )
    }
}

@Preview(apiLevel = 33, showBackground = true)
@Composable
fun MessageRowTwoNamePreview() {
    MessagingTheme {
        MessageRow(
            messageThread =
            MessageThread(
                id = "1",
                participants = listOf("First Name", "Second Name"),
                message = "Hello, world with a super long message and some will be cut off!",
                time = System.currentTimeMillis()
            )
        )
    }
}

@Preview(apiLevel = 33, showBackground = true)
@Composable
fun MessageRowThreeNamePreview() {
    MessagingTheme {
        MessageRow(
            messageThread =
            MessageThread(
                id = "1",
                participants = listOf("First Name", "SecondName", "Third Name"),
                message = "Hello, world with a super long message and some will be cut off!",
                time = System.currentTimeMillis()
            )
        )
    }
}

@Preview(apiLevel = 33, showBackground = true)
@Composable
fun MessageRowFourNamePreview() {
    MessagingTheme {
        MessageRow(
            messageThread =
            MessageThread(
                id = "1",
                participants = listOf("First Name", "Second Name", "Third Name", "Fourth Name"),
                message = "Hello, world with a super long message and some will be cut off!",
                time = System.currentTimeMillis()
            )
        )
    }
}

@Preview(apiLevel = 33, showBackground = true)
@Composable
fun MessageRowManyPreview() {
    MessagingTheme {
        MessageRow(
            messageThread =
            MessageThread(
                id = "1",
                participants = listOf("FirstName", "Second Name", "Third Name", "Fourth Name", "Fifth Name"),
                message = "Hello, world with a super long message and some will be cut off!",
                time = System.currentTimeMillis()
            )
        )
    }
}