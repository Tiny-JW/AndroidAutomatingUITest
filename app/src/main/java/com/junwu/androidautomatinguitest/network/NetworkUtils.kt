package com.junwu.androidautomatinguitest.network

import com.junwu.androidautomatinguitest.network.api.UserApi
import com.junwu.androidautomatinguitest.network.model.OTPRequest
import com.junwu.androidautomatinguitest.network.model.PostV1OTPResponse
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

private const val baseUrl = "http://127.0.0.1:8080"

class NetworkUtils {

    private val mMoshi = Moshi.Builder().build()
    private val mOkHttpClient = OkHttpClient.Builder().build()
    private val moshiConverterFactory = MoshiConverterFactory.create(mMoshi)
    private val mRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(mOkHttpClient)
        .addConverterFactory(moshiConverterFactory)
        .build()

    suspend fun requestOtp(phoneNumber: String): PostV1OTPResponse {
        return getUserApi().postV1Otp(OTPRequest(phoneNumber))
    }

    private fun getUserApi() = mRetrofit.create(UserApi::class.java)

}