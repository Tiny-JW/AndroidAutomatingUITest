package com.junwu.androidautomatinguitest.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OTPRequest(
    @Json(name = "phoneNumber")
    val phoneNumber: String,
)

