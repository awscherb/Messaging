package com.awscherb.messaging.db

import androidx.room.TypeConverter
import com.awscherb.messaging.data.MessageType
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

object MessagingTypeConverters {

    private val gson by lazy { GsonBuilder().create() }

    @[TypeConverter JvmStatic]
    fun fromStringList(list: List<String>): String {
        return gson.toJson(list)
    }

    @[TypeConverter JvmStatic]
    fun toStringList(list: String): List<String>{
        val listType = object : TypeToken<List<String>?>() {
        }.type
        return gson.fromJson(list, listType)
    }

    @[TypeConverter JvmStatic]
    fun fromMessageType(messageType: MessageType) = messageType.ordinal

    @[TypeConverter JvmStatic]
    fun toMessageType(ordinal: Int) = MessageType.entries[ordinal]
}