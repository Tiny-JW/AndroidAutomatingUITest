package com.junwu.androidautomatinguitest.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostV1OTPResponse(
    @Json(name = "retryAfterSeconds")
    val retryAfterSeconds: Long
)


