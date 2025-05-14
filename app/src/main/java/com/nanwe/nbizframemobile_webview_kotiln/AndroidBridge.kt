package com.nanwe.nbizframemobile_webview_kotiln

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class AndroidBridge(private val context: Context, private val webView: WebView) {

    private val handler = Handler(Looper.getMainLooper())
    private val TAG = "AndroidBridge"

    @JavascriptInterface
    fun callMethod(uid: String, svcid: String, paramString: String?) {
        Log.d(TAG, "uid : $uid")
        Log.d(TAG, "svcid : $svcid")
        Log.d(TAG, "paramString : $paramString")

        val param = if (!paramString.isNullOrEmpty()) JSONObject(paramString) else JSONObject()

        when (svcid) {
            "PERMISSION" -> {
                send(uid, svcid, AppConstants.CODE_SUCCESS, "PERMISSION")
            }

            "LOGIN" -> {
                val userId = param.optString("userId")
                val token = AppPreferenceManager.getString("token")
                Log.d(TAG, "userId : $userId")
                Log.d(TAG, "token : $token")

                if (userId.isNotEmpty() && token.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        PushTokenManager.sendToken(context, AppConstants.PUSH_SAVE_TOKEN_URL, token, userId)
                    }
                    send(uid, svcid, AppConstants.CODE_SUCCESS, "LOGIN SUCCESS")
                } else {
                    send(uid, svcid, AppConstants.CODE_ERROR, "parameter null")
                }
            }

            "LOGOUT" -> {
                AppPreferenceManager.removeKey("accessToken")
                AppPreferenceManager.removeKey("refreshToken")

                val userId = param.optString("userId")
                val token = AppPreferenceManager.getString("token")
                Log.d(TAG, "userId : $userId")
                Log.d(TAG, "token : $token")

                if (userId.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        PushTokenManager.sendToken(context, AppConstants.PUSH_REMOVE_TOKEN_URL, token, userId)
                    }
                    send(uid, svcid, AppConstants.CODE_SUCCESS, "LOGOUT SUCCESS")
                } else {
                    send(uid, svcid, AppConstants.CODE_ERROR, "parameter null")
                }
            }

            "SET_ACCESS_TOKEN" -> {
                val accessToken = param.optString("accessToken")
                val refreshToken = param.optString("refreshToken")
                Log.d(TAG, "accessToken : $accessToken")
                Log.d(TAG, "refreshToken : $refreshToken")

                if (accessToken.isNotEmpty()) {
                    AppPreferenceManager.setString("accessToken", accessToken)
                    AppPreferenceManager.setString("refreshToken", refreshToken)
                    send(uid, svcid, AppConstants.CODE_SUCCESS, "SUCCESS")
                } else {
                    send(uid, svcid, AppConstants.CODE_ERROR, "accessToken null")
                }
            }

            "GET_ACCESS_TOKEN" -> {
                val accessToken = AppPreferenceManager.getString("accessToken")
                val refreshToken = AppPreferenceManager.getString("refreshToken")
                Log.d(TAG, "accessToken : $accessToken")
                Log.d(TAG, "refreshToken : $refreshToken")

                if (accessToken.isNotEmpty() && refreshToken.isNotEmpty()) {
                    val rVal = JSONObject().apply {
                        put("accessToken", accessToken)
                        put("refreshToken", refreshToken)
                    }
                    send(uid, svcid, AppConstants.CODE_SUCCESS, rVal)
                } else {
                    send(uid, svcid, AppConstants.CODE_ERROR, "accessToken null")
                }
            }

            else -> {
                send(uid, svcid, AppConstants.CODE_ERROR, "not allow service")
            }
        }
    }

    private fun send(uid: String, svcid: String, reason: Int, retval: Any) {
        handler.post {
            try {
                val obj = JSONObject().apply {
                    put(AppConstants.UID, uid)
                    put(AppConstants.SVCID, svcid)
                    put(AppConstants.REASON, reason)
                    put(AppConstants.RETVAL, retval)
                }
                webView.loadUrl("javascript:NativeAppManager.callbackWebApp('${obj.toString()}');")
            } catch (e: Exception) {
                Log.d(TAG, e.localizedMessage ?: "send error")
            }
        }
    }
}
