package com.awscherb.messaging.pager

import android.content.Context
import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.awscherb.messaging.ui.thread.common.Message

class SmsPager(val context: Context) {
    fun getPagerFlow(threadId: String) = Pager(
        config = PagingConfig(pageSize = SmsPagingSource.LIMIT),
        pagingSourceFactory = { SmsPagingSource(context, threadId) }
    ).flow

    class SmsPagingSource(
        val context: Context,
        val thread: String
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
            context.contentResolver.query(
                Uri.parse("content://sms"),
                null,
                "thread_id=$thread",
                null, "DATE DESC LIMIT $LIMIT" + if (skip > 0) " OFFSET $skip" else ""
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

            return LoadResult.Page(
                data = messagesList,
                prevKey = if (page == 0) null else page - 1,
                nextKey = page + 1
            )

        }
    }
}