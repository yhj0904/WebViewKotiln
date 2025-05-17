package com.nanwe.nbizframemobile_webview_kotiln.api

import com.nanwe.nbizframemobile_webview_kotiln.model.AuthRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("login")
    suspend fun login(@Body request: AuthRequest): Response<Void>
}
