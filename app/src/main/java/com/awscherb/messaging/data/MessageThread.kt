package com.awscherb.messaging.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MessageThread(
    @PrimaryKey
    val threadId: String,
    val message: String,
    val date: Long,
    val participants: List<String>,
    val read: Boolean,
    val fromMe: Boolean,
    val threadType: MessageType
)