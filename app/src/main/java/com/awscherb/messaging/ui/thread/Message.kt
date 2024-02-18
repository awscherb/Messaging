package com.awscherb.messaging.ui.thread

data class Message(
    val id: String,
    val text: String,
    val fromMe: Boolean,
    val contact: String?,
    val date: Long,
    val data: String? = null
)
