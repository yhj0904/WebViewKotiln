package com.nanwe.nbizframemobile_webview_kotiln

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.messaging.FirebaseMessaging
import com.nanwe.nbizframemobile_webview_kotiln.api.RetrofitClient
import com.nanwe.nbizframemobile_webview_kotiln.model.AuthRequest
import com.nanwe.nbizframemobile_webview_kotiln.model.TokenRequest
import kotlinx.coroutines.launch

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var toast: Toast? = null
    private var backKeyPressedTime = 0L

    private val TAG = "WebViewActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPreferenceManager.init(this)
        setContentView(R.layout.activity_web_view)

        val initNoticeNo = intent?.getStringExtra("initNoticeNo").orEmpty()
        val targetUrl = if (initNoticeNo.isNotEmpty())
            "${AppConstants.APP_URL}/noticeView.do?noticeNo=$initNoticeNo" else AppConstants.APP_URL

        loginWithJwt {
            registerFcmToken { setupWebView(targetUrl) }
        }
    }

    /* ---------------- JWT 로그인 → access 헤더 추출 ----------------*/
    private fun loginWithJwt(onSuccess: () -> Unit) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.auth.login(AuthRequest("nauri", "1234"))
                if (res.isSuccessful) {
                    val accessToken = res.headers()["access"] ?: ""
                    getSecurePrefs().edit().putString("accessToken", accessToken).apply()
                    onSuccess()
                } else {
                    Log.e(TAG, "JWT 로그인 실패: ${res.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "JWT 로그인 오류: ${e.localizedMessage}")
            }
        }
    }

    /* ---------------- FCM 토큰 서버 등록 ----------------*/
    private fun registerFcmToken(onSuccess: () -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e(TAG, "FCM 토큰 취득 실패", task.exception)
                return@addOnCompleteListener
            }
            val fcm = task.result ?: return@addOnCompleteListener
            AppPreferenceManager.setString("token", fcm)

            val raw = getSecurePrefs().getString("accessToken", "") ?: ""
            val bearer = if (raw.isNotEmpty()) "Bearer $raw" else null

            val req = TokenRequest(
                appId = AppConstants.APP_ID,
                userId = "nauri", //AppPreferenceManager.getString("userId")
                deviceId = "ANDROID",
                fcmToken = fcm
            )
            lifecycleScope.launch {
                try {
                    val r = RetrofitClient.push.registerToken(bearer, req)
                    if (r.isSuccessful) Log.d(TAG, "FCM 토큰 등록 성공")
                    else Log.e(TAG, "FCM 토큰 등록 실패: ${r.code()}")
                } catch (e: Exception) {
                    Log.e(TAG, "등록 오류", e)
                } finally { onSuccess() }
            }
        }
    }

    private fun setupWebView(url: String) {
        webView = findViewById(R.id.webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        webView.addJavascriptInterface(AndroidBridge(this, webView), "AOSWebApp")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return when {
                    url.isNullOrBlank() -> false
                    url.startsWith("tel:") -> {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        true
                    }
                    else -> {
                        view?.loadUrl(url)
                        true
                    }
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                showDialog("알림", message ?: "", result)
                return true
            }

            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                showDialog("확인", message ?: "", result, isConfirm = true)
                return true
            }
        }

        webView.loadUrl(url)
    }

    private fun showDialog(title: String, message: String, result: JsResult?, isConfirm: Boolean = false) {
        AlertDialog.Builder(this).apply {
            setTitle(title)
            setMessage(message)
            setCancelable(false)
            setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
            if (isConfirm) {
                setNegativeButton(android.R.string.cancel) { _, _ -> result?.cancel() }
            }
            show()
        }
    }

    override fun onBackPressed() {
        val now = System.currentTimeMillis()
        if (now > backKeyPressedTime + 2000) {
            backKeyPressedTime = now
            toast = Toast.makeText(this, "'뒤로' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT)
            toast?.show()
        } else {
            toast?.cancel()
            super.onBackPressed()
        }
    }

    private fun saveTokens(response: retrofit2.Response<*>) {
        val accessToken = response.headers()["access"] ?: ""
        val refreshToken = response.headers()["Set-Cookie"]
            ?.substringAfter("refreshToken=")
            ?.substringBefore(";") ?: ""

        val prefs = getSecurePrefs()
        prefs.edit().apply {
            putString("accessToken", accessToken)
            putString("refreshToken", refreshToken)
            apply()
        }
    }

    private fun getSecurePrefs(): SharedPreferences {
        val mk = MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
            this, "secure_prefs", mk,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
