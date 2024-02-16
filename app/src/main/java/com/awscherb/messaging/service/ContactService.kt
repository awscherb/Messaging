package com.awscherb.messaging.service

import com.awscherb.messaging.data.CanonicalContact

interface ContactService {

    suspend fun fetchContact(address: String): CanonicalContact?
}