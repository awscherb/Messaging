package com.awscherb.messaging.service

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.LruCache
import com.awscherb.messaging.data.CanonicalContact
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject


class ContactHandler @Inject constructor(
    @ApplicationContext private val context: Context
) : ContactService {

    private val cache = LruCache<String, CanonicalContact>(50)
    override suspend fun fetchContact(address: String): CanonicalContact? {
        when (val cached = cache[address]) {
            null -> {
                return context.contentResolver.query(
                    Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, address),
                    arrayOf("display_name", "_id"),
                    null,
                    null,
                    null
                )?.use {
                    it.moveToFirst()
                    if (!it.isAfterLast) {
                        val id = it.getString(it.getColumnIndexOrThrow("_id"))
                        val display = it.getString(it.getColumnIndexOrThrow("display_name"))

                        CanonicalContact(id = id, address = address, displayName = display).also {
                            cache.put(address, it)
                        }
                    } else {
                        null
                    }
                }
            }

            else -> {
                return cached
            }
        }

    }
}