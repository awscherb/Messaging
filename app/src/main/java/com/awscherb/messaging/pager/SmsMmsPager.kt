package com.awscherb.messaging.pager

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.awscherb.messaging.content.MmsHelper
import com.awscherb.messaging.ui.thread.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class SmsMmsPager(
    val context: Context,
    val mmsHelper: MmsHelper
) {
    fun getPagerFlow(threadId: String) = Pager(
        config = PagingConfig(pageSize = SmsMmsPagingSource.LIMIT),
        pagingSourceFactory = { SmsMmsPagingSource(context, threadId, mmsHelper) }
    ).flow

    class SmsMmsPagingSource(
        val context: Context,
        val thread: String,
        val mmsHelper: MmsHelper
    ) : PagingSource<Int, Message>() {

        companion object {
            internal const val LIMIT = 20
        }

        override fun getRefreshKey(state: PagingState<Int, Message>): Int? {
            return null
        }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Message> {
            val loadStart = System.currentTimeMillis()
            val page = params.key ?: 0

            val lowerBound = page * LIMIT
            val upperBound = lowerBound + LIMIT

            val skip = page * LIMIT
            val messagesList = mutableListOf<Message>()
            val smsIds = mutableListOf<String>()
            val mmsIds = mutableListOf<String>()
            val queryStart = System.currentTimeMillis()

            coroutineScope {
                withContext(Dispatchers.IO) {
                    context.contentResolver.query(
                        Uri.parse("content://mms-sms/conversations/$thread"),
                        arrayOf("_id", "date", "date_sent", "read", "thread_id", "transport_type"),
                        null,
                        null,
                        "DATE DESC"
                    )?.use {
                        it.moveToFirst()
                        var i = 0
                        while (!it.isAfterLast && i < upperBound) {
                            val id = it.getString(it.getColumnIndexOrThrow("_id"))
                            val transportType =
                                it.getString(it.getColumnIndexOrThrow("transport_type"))
                            var date = it.getLong(it.getColumnIndexOrThrow("date"))

                            if (date < 1000000000000) {
                                date *= 1000
                            }

                            if (i >= lowerBound) {
                                when (transportType) {
                                    "sms" -> smsIds.add(id)
                                    "mms" -> mmsIds.add(id)
                                }
                            }

                            i++
                            it.moveToNext()

                        }
                        println("conversations took ${System.currentTimeMillis() - queryStart}")
                    }
                }
            }

            coroutineScope {
                val mms = async {
                    withContext(Dispatchers.IO) {
                        val mmsStart = System.currentTimeMillis()
                        val list = mmsHelper.getMessagesForMms(mmsIds)
                        println("mms total took ${System.currentTimeMillis() - mmsStart}")
                        list
                    }
                }

                val sms = async {
                    withContext(Dispatchers.IO) {
                        val smsStart = System.currentTimeMillis()
                        val list = context.contentResolver.query(
                            Telephony.Sms.CONTENT_URI,
                            null,
                            "_id IN (${smsIds.joinToString(",")})",
                            null,
                            null,
                            null
                        )?.use {
                            val smsList = mutableListOf<Message>()
                            it.moveToFirst()
                            while (!it.isAfterLast) {
                                val message = it.getString(it.getColumnIndexOrThrow("body"))
                                val id = it.getString(it.getColumnIndexOrThrow("_id"))
                                val fromMe = it.getInt(it.getColumnIndexOrThrow("type")) == 2
                                val date = it.getLong(it.getColumnIndexOrThrow("date"))
                                smsList += Message(id, message, fromMe, null, date)
                                it.moveToNext()
                            }
                            smsList
                        } ?: emptyList()
                        println("Sms took ${System.currentTimeMillis() - smsStart}")
                        list
                    }
                }

                messagesList += listOf(sms, mms).awaitAll().foldRight(emptyList()) { l, r -> l + r }
            }

            val sort = System.currentTimeMillis()
            messagesList.sortBy { -it.date }
            println("Sort took ${System.currentTimeMillis() - sort}")
            println("Load total took ${System.currentTimeMillis() - loadStart}")
            return LoadResult.Page(
                data = messagesList,
                prevKey = if (page == 0) null else page - 1,
                nextKey = page + 1
            )

        }
    }
}