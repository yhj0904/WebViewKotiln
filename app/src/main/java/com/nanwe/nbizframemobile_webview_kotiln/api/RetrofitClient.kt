package com.nanwe.nbizframemobile_webview_kotiln.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "http://192.168.10.54:8080/"

    /**
     * 공통 OkHttpClient – 현재는 특수 헤더나 인터셉터 없이 사용.
     *   • accessToken 을 붙여야 하는 호출은, 요청 만들 때 수동으로 헤더를 추가하세요.
     */
    private val plainClient: OkHttpClient = OkHttpClient.Builder().build()

    /**
     * Push 관련 API (FCM 토큰 등록/삭제)
     */
    val push: PushApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(plainClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PushApi::class.java)
    }

    /**
     * Auth 관련 API – reissue 제거, login 만 사용
     */
    val auth: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(plainClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }
}
