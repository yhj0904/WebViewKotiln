package com.nanwe.nbizframemobile_webview_kotiln.api

import com.nanwe.nbizframemobile_webview_kotiln.model.TokenRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface PushApi {
    @POST("api/token/register")
    suspend fun registerToken(
        @Header("Authorization") bearer: String?,
        @Body body: TokenRequest
    ): Response<Void>

    @POST("api/token/delete")
    suspend fun deleteToken(
        @Header("Authorization") bearer: String?,
        @Body body: TokenRequest
    ): Response<Void>
}