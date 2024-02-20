package com.awscherb.messaging.pager

import android.content.Context
import android.database.DatabaseUtils
import android.net.Uri
import android.provider.Telephony
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.awscherb.messaging.content.MmsHelper
import com.awscherb.messaging.dao.ThreadMessageRecordDao
import com.awscherb.messaging.data.Message
import com.awscherb.messaging.data.MessageType
import com.awscherb.messaging.data.ThreadMessageRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class SmsMmsPager(
    val context: Context,
    val threadMessageRecordDao: ThreadMessageRecordDao,
    val mmsHelper: MmsHelper
) {
    fun getPagerFlow(threadId: String) = Pager(
        config = PagingConfig(pageSize = SmsMmsPagingSource.LIMIT),
        pagingSourceFactory = {
            SmsMmsPagingSource(
                context = context,
                thread = threadId,
                threadMessageRecordDao = threadMessageRecordDao,
                mmsHelper = mmsHelper
            )
        }
    ).flow

    class SmsMmsPagingSource(
        val context: Context,
        val thread: String,
        val threadMessageRecordDao: ThreadMessageRecordDao,
        val mmsHelper: MmsHelper
    ) : PagingSource<Int, Message>() {

        companion object {
            internal const val LIMIT = 25
        }

        override fun getRefreshKey(state: PagingState<Int, Message>): Int? {
            return null
        }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Message> {
            val page = withContext(Dispatchers.IO) {

                val loadStart = System.currentTimeMillis()
                val page = params.key ?: 0

                val skip = page * LIMIT
                val messagesList = mutableListOf<Message>()
                val smsIds = mutableListOf<String>()
                val mmsIds = mutableListOf<String>()
                val queryStart = System.currentTimeMillis()
                println("thread is ${Thread.currentThread().name}")

                val records = threadMessageRecordDao.fetchRecords(
                    threadId = thread,
                    limit = LIMIT,
                    offset = skip
                )
                println("fetch result was $records")

                records.forEach {
                    when (it.type) {
                        MessageType.SMS -> smsIds.add(it.id)
                        MessageType.MMS -> mmsIds.add(it.id)
                    }
                }

                coroutineScope {
                    val mms = async {
                        withContext(Dispatchers.IO) {
                            val mmsStart = System.currentTimeMillis()
                            val list = mmsHelper.getMessagesForMms(thread, mmsIds)
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
                                    smsList += Message(
                                        id = id,
                                        text = message,
                                        fromMe = fromMe,
                                        data = null,
                                        date = date,
                                        threadId = thread,
                                        contact = null
                                    )
                                    it.moveToNext()
                                }
                                smsList
                            } ?: emptyList()
                            println("Sms took ${System.currentTimeMillis() - smsStart}")
                            list
                        }
                    }

                    messagesList += listOf(sms, mms).awaitAll()
                        .foldRight(emptyList()) { l, r -> l + r }
                }

                val sort = System.currentTimeMillis()
                messagesList.sortBy { -it.date }

                println("Sort took ${System.currentTimeMillis() - sort}")
                println("Load total took ${System.currentTimeMillis() - loadStart}")

                LoadResult.Page(
                    data = messagesList,
                    prevKey = if (page == 0) null else page - 1,
                    nextKey = if (messagesList.size < LIMIT) null else page + 1
                )
            }

            return page

        }
    }
}