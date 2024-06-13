package com.bignerdranch.android.client.websocketclient

import kotlinx.serialization.Serializable

@Serializable
data class Request(val requestId: Long, val status: Int)

