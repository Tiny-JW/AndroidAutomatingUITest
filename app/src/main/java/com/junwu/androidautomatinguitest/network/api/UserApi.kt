package com.junwu.androidautomatinguitest.network.api

import com.junwu.androidautomatinguitest.network.model.OTPRequest
import com.junwu.androidautomatinguitest.network.model.PostV1OTPResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface UserApi {

    @POST("v1/otp")
    suspend fun postV1Otp(@Body otPRequest: OTPRequest): PostV1OTPResponse
}