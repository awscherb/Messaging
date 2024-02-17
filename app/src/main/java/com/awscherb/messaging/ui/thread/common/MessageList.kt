package com.awscherb.messaging.ui.thread.common

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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Dimension
import coil.size.OriginalSize
import coil.size.Size
import com.awscherb.messaging.ui.theme.Pink80
import com.awscherb.messaging.ui.theme.Purple80
import java.io.IOException
import java.io.InputStream


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
            modifier = Modifier.align(aligment)
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
        message.contact?.let {
            Text(
                text = message.contact,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .align(aligment)
            )
        }
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
            Message("1", "Hello, world!", true, "3125550690", 0L),
            Message("2", "hey back!", false, "Contact", 0L, ""),
            Message("3", "", false, null, 0L, "sata"),
        )
    )
}