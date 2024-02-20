package com.awscherb.messaging.data

import androidx.compose.ui.tooling.preview.Preview
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * corresponds with metadata from content://mms-sms/conversations/$threadID
 */
@Entity
data class ThreadMessageRecord(
    @PrimaryKey
    val id: String,
    val date: Long,
    val threadId: String,
    val type: MessageType
)
