package com.nanwe.nbizframemobile_webview_kotiln.model

data class TokenRequest(
    val appId: String,
    val userId: String,
    val deviceId: String,
    val fcmToken: String
)