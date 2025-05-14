package com.nanwe.nbizframemobile_webview_kotiln

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log

class MainActivity : Activity() {

    private val TAG = "MainActivity"
    private var initNoticeNo: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        intent?.let {
            val appId = it.getStringExtra("appId")
            val noticeNo = it.getStringExtra("noticeNo")

            initNoticeNo = if (!appId.isNullOrEmpty() && !noticeNo.isNullOrEmpty()) {
                noticeNo
            } else {
                ""
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            moveWebViewActivity()
        }, 2000)
    }

    private fun moveWebViewActivity() {
        val intent = Intent(this, WebViewActivity::class.java)
        intent.putExtra("initNoticeNo", initNoticeNo)
        startActivity(intent)
        finish()
    }
}