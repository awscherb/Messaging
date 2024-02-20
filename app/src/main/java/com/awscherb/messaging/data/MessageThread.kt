package com.awscherb.messaging.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MessageThread(
    @PrimaryKey
    val threadId: String,
    val message: String,
    val date: Long,
    // Resolved contact names, i.e. Bob Smith
    val participants: List<String>,
    val addresses: List<String>,
    val read: Boolean,
    val fromMe: Boolean,
    val threadType: MessageType
) {

    companion object {
        private fun String.toFirstName(): String {
            return when {
                !this.contains(" ") -> this
                else -> this.split(" ")[0]
            }
        }
    }

    fun getTitle(): String {
        return when {
            participants.isEmpty() -> ""
            else -> {
                participants.joinToString(separator = ", ") { it.toFirstName() }
            }
        }
    }
}