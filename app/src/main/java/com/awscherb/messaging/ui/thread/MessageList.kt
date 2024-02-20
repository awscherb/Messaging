package com.awscherb.messaging.ui.thread

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.awscherb.messaging.data.Message
import com.awscherb.messaging.ui.util.TimeHolder
import java.io.IOException
import java.io.InputStream
import java.util.Date


@Composable
fun MessageList(
    modifier: Modifier = Modifier,
    messages: List<Message>
) {
    LazyColumn(modifier = modifier, reverseLayout = true) {
        items(messages, key = { it.id }, contentType = { it.fromMe }) {
            MessageBubble(message = it)
        }
    }
}

@Composable
fun MessageBubble(
    message: Message
) {
    val screenWidth = (LocalConfiguration.current.screenWidthDp)
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .fillMaxWidth()
    ) {
        val aligment: Alignment.Horizontal
        val color: Color
        if (message.fromMe) {
            aligment = Alignment.End
            color = MaterialTheme.colorScheme.primaryContainer
        } else {
            aligment = Alignment.Start
            color = MaterialTheme.colorScheme.secondaryContainer
        }

        Column(
            modifier = Modifier
                .align(aligment)
                .widthIn(0.dp, (screenWidth * .75).dp)
        ) {
            message.data?.let { partId ->

                val bitmap = getMmsImage(partId, LocalContext.current) ?: return@let
                AsyncImage(
                    model = ImageRequest.Builder(
                        LocalContext.current
                    )
                        .data(getMmsImage(partId, LocalContext.current))
                        .build(),
                    contentDescription = "Image",
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .width((screenWidth / 2).dp)
                        .height((((screenWidth / 2) * bitmap.height) / bitmap.width).dp)
                        .align(aligment)
                )
            }
            if (message.text.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(color)
                        .align(aligment)
                ) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        text = message.text,
                    )
                }
            }
        }

        val contact = message.contact
        val prefix = if (contact != null) "$contact • " else ""
        Text(
            text = prefix + TimeHolder.prettyTime.format(Date(message.date)),
            modifier = Modifier
                .padding(top = 4.dp)
                .align(aligment)
        )
    }
}

private fun getMmsImage(_id: String, context: Context): Bitmap? {
    val partURI = Uri.parse("content://mms/part/$_id")
    var `is`: InputStream? = null
    var bitmap: Bitmap? = null
    try {
        `is` = context.contentResolver.openInputStream(partURI)
        bitmap = BitmapFactory.decodeStream(`is`)
    } catch (e: IOException) {
    } finally {
        if (`is` != null) {
            try {
                `is`.close()
            } catch (e: IOException) {
            }
        }
    }
    return bitmap
}


@Preview(showSystemUi = true)
@Composable
fun MessageListPreview() {
    MessageList(
        messages = listOf(
            Message("1", "1","Hello, world!", true, "3125550690", System.currentTimeMillis()),
            Message(
                "2",
                "1",
                "hey back!",
                false,
                "Contact",
                System.currentTimeMillis() - 5 * 60 * 1000,
                ""
            ),
            Message(
                "3",
                "1",
                "text",
                false,
                null,
                System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000,
                "sata"
            ),
        )
    )
}