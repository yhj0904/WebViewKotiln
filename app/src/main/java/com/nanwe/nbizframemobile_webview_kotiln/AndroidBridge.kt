package com.nanwe.nbizframemobile_webview_kotiln

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nanwe.nbizframemobile_webview_kotiln.api.RetrofitClient
import com.nanwe.nbizframemobile_webview_kotiln.model.TokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class AndroidBridge(private val context: Context, private val webView: WebView) {

    private val handler = Handler(Looper.getMainLooper())
    private val TAG = "AndroidBridge"

    private fun accessHeader(): String? {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        val prefs = EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val token = prefs.getString("accessToken", "") ?: ""
        return if (token.isNotEmpty()) "Bearer $token" else null
    }

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
                val userId = "nauri" // 임시 AppPreferenceManager.getString("userId")
                val token = AppPreferenceManager.getString("token")
                val deviceId = "ANDROID"

                if (userId.isNotEmpty() && token.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            RetrofitClient.push.registerToken(
                                accessHeader(),
                                TokenRequest(AppConstants.APP_ID, userId, deviceId, token)
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Retrofit 오류: ${e.localizedMessage}")
                        }
                    }
                    send(uid, svcid, AppConstants.CODE_SUCCESS, "LOGIN SUCCESS")
                } else {
                    send(uid, svcid, AppConstants.CODE_ERROR, "parameter null")
                }
            }

            "LOGOUT" -> {
                AppPreferenceManager.removeKey("accessToken")
                AppPreferenceManager.removeKey("refreshToken")

                val userId = "nauri" // or param.optString("userId")
                val token = AppPreferenceManager.getString("token")
                val deviceId = "ANDROID"

                if (userId.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            RetrofitClient.push.deleteToken(accessHeader(),
                                TokenRequest(AppConstants.APP_ID, userId, deviceId, token)
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Retrofit 오류: ${e.localizedMessage}")
                        }
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
