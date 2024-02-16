package com.awscherb.messaging.ui.base

sealed class Destination(
    val label: String,
    val dest: String
) {
    data object Messages : Destination(
        label = "Messages",
        dest = "messages"
    )

    data object SmsThread : Destination(
        label = "Thread",
        dest = "sms/{id}"
    )

    data object MmsThread : Destination(
        label = "Thread",
        dest = "mms/{id}"
    )
}