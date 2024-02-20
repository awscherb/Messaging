package com.awscherb.messaging.service

import android.content.Context
import com.awscherb.messaging.dao.ThreadMessageRecordDao
import com.awscherb.messaging.data.ThreadMessageRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ThreadRecordService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val threadMessageRecordDao: ThreadMessageRecordDao,
) {

}