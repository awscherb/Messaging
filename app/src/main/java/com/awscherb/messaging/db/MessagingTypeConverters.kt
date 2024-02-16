package com.awscherb.messaging.db

import androidx.room.TypeConverter
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
}