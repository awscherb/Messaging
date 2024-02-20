package com.awscherb.messaging.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Message(
    @PrimaryKey
    val id: String,
    val threadId: String,
    val text: String,
    val fromMe: Boolean,
    val contact: String?,
    val date: Long,
    val data: String? = null
)
