package com.nanwe.nbizframemobile_webview_kotiln

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
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
        val targetUrl = if (initNoticeNo.isNotEmpty()) {
            "${AppConstants.APP_URL}/noticeView.do?noticeNo=$initNoticeNo"
        } else {
            AppConstants.APP_URL
        }

        fetchFirebaseToken()
        setupWebView(targetUrl)
    }

    private fun fetchFirebaseToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result ?: return@addOnCompleteListener
                Log.d(TAG, "FCM Token: $token")
                AppPreferenceManager.setString("token", token)

                val userId = AppPreferenceManager.getString("userId")

                lifecycleScope.launch {
                    PushTokenManager.sendToken(
                        applicationContext,
                        AppConstants.PUSH_SAVE_TOKEN_URL,
                        token,
                        userId
                    )
                }
            } else {
                Log.e(TAG, "FCM Token fetch failed: ${task.exception}")
            }
        }
    }


    @Suppress("SetJavaScriptEnabled")
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
                url ?: return false
                return if (url.startsWith("tel:")) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    true
                } else {
                    view?.loadUrl(url)
                    true
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

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val now = System.currentTimeMillis()
        if (now > backKeyPressedTime + 2000) {
            backKeyPressedTime = now
            toast = Toast.makeText(this, "'뒤로' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT)
            toast?.show()
        } else {
            toast?.cancel()
            finish()
        }
    }
}
