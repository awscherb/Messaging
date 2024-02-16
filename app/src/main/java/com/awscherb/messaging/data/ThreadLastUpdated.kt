package com.awscherb.messaging.data

// to check which threads to update
data class ThreadLastUpdated(
    val threadId: String,
    val date: Long
)
