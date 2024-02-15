package com.awscherb.messaging.ui.messages

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.atMostWrapContent
import com.awscherb.messaging.MessageThread
import com.awscherb.messaging.ui.theme.MessagingTheme
import com.awscherb.messaging.ui.theme.Purple80
import org.ocpsoft.prettytime.PrettyTime
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

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

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Purple80)
            )

            Text(
                text = messageThread.from.getPreviewText(),
            )
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

private fun String.getPreviewText(): String {
    return if (!this.contains(" ")) {
        return this
    } else {
        val parts = split(" ")
        if (parts.size == 1) {
            parts[0].getPreviewText()
        } else {
            parts[0][0].toString().capitalize() + parts[1][0].toString().capitalize()
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
                from = "First Name",
                message = "Hello, world with a super long message and some will be cut off!",
                time = System.currentTimeMillis()
            )
        )
    }
}