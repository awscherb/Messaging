package com.awscherb.messaging.ui.messages

import android.text.format.DateUtils
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
        Text(
            modifier = Modifier
                .constrainAs(who) {
                    linkTo(
                        top = parent.top,
                        bottom = parent.bottom,
                        end = message.start,
                        start = parent.start
                    )
                }
                .padding(32.dp)
                .drawBehind {
                    drawCircle(
                        color = Purple80,
                        radius = this.size.maxDimension
                    )
                },
            text = messageThread.from.substring(0..2),
        )

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
            maxLines = 1,
            overflow = TextOverflow.Clip
        )

        Text(
            modifier = Modifier
                .constrainAs(time) {
                    linkTo(
                        top = message.top,
                        bottom = message.top,
                        end = parent.end,
                        start = message.end
                    )

                }
                .padding(end = 16.dp),
            text = PrettyTime().format(Date(messageThread.time)),
            maxLines = 1,


            )

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
                from = "3125550690",
                message = "Hello, world!",
                time = System.currentTimeMillis()
            )
        )
    }
}