package com.awscherb.messaging.pager

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.awscherb.messaging.content.MmsHelper
import com.awscherb.messaging.service.ContactService
import com.awscherb.messaging.ui.thread.common.Message

class SmsMmsPager(
    val context: Context,
    val contactService: ContactService
) {
    fun getPagerFlow(threadId: String) = Pager(
        config = PagingConfig(pageSize = SmsMmsPagingSource.LIMIT),
        pagingSourceFactory = { SmsMmsPagingSource(context, threadId, contactService) }
    ).flow

    class SmsMmsPagingSource(
        val context: Context,
        val thread: String,
        val contactService: ContactService
    ) : PagingSource<Int, Message>() {

        companion object {
            internal const val LIMIT = 20
        }

        override fun getRefreshKey(state: PagingState<Int, Message>): Int? {
            return null
        }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Message> {
            val page = params.key ?: 0

            val skip = page * LIMIT
            val messagesList = mutableListOf<Message>()
            val smsIds = mutableListOf<String>()
            val mmsIds = mutableListOf<String>()
            val queryStart = System.currentTimeMillis()
            context.contentResolver.query(
                Uri.parse("content://mms-sms/conversations/$thread"),
                arrayOf("_id", "date", "date_sent", "read", "thread_id", "transport_type"),
                null,
                null,
                "DATE DESC"
            )?.use {
                it.moveToFirst()
                var i = 0
                while (!it.isAfterLast) {
                    i++
                    val id = it.getString(it.getColumnIndexOrThrow("_id"))
                    val transportType = it.getString(it.getColumnIndexOrThrow("transport_type"))
                    var date = it.getLong(it.getColumnIndexOrThrow("date"))

                    if (date < 1000000000000) {
                        date *= 1000
                    }

                    when (transportType) {
                        "sms" -> smsIds.add(id)
                        "mms" -> mmsIds.add(id)
                    }
                    it.moveToNext()

                }
                println("query end is ${System.currentTimeMillis() - queryStart}")
            }

            val mms = MmsHelper.getMessagesForMms(context, mmsIds, contactService)

            println("mms count is ${mms.size}")

            messagesList += mms

            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                "_id IN (${smsIds.joinToString(",")})",
                null,
                null,
                null
            )?.use {
                it.moveToFirst()
                while (!it.isAfterLast) {
                    val message = it.getString(it.getColumnIndexOrThrow("body"))
                    val id = it.getString(it.getColumnIndexOrThrow("_id"))
                    val fromMe = it.getInt(it.getColumnIndexOrThrow("type")) == 2
                    val date = it.getLong(it.getColumnIndexOrThrow("date"))
                    messagesList += Message(id, message, fromMe, null, date)
                    it.moveToNext()
                }
            }

            messagesList.sortBy { -it.date }

            return LoadResult.Page(
                data = messagesList,
                prevKey = if (page == 0) null else page - 1,
                nextKey = page + 1
            )

        }
    }
}