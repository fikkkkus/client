package com.bignerdranch.android.client.websocketclient

import kotlinx.serialization.Serializable

@Serializable
data class GestureParams(val requestId: Long, val direction: Int, val distance: Int)
