package com.nanwe.nbizframemobile_webview_kotiln

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object PushTokenManager {
    private const val TAG = "PushTokenManager"

    suspend fun sendToken(context: Context, url: String, token: String, userId: String = "")
            = withContext(Dispatchers.IO) {
        var responseStr = ""
        var conn: HttpURLConnection? = null

        try {
            val targetUrl = URL(url)

            if (url.startsWith("https://")) {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate>? = null
                })

                val sc = SSLContext.getInstance("SSL")
                sc.init(null, trustAllCerts, SecureRandom())
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
                conn = targetUrl.openConnection() as HttpsURLConnection
            } else {
                conn = targetUrl.openConnection() as HttpURLConnection
            }

            conn.useCaches = false
            conn.doOutput = true
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-type", "application/json")

            val jsonString = """
                {
                    "appId":"${AppConstants.APP_ID}",
                    "token":"$token",
                    "userId":"$userId"
                }
            """.trimIndent()

            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(jsonString) }

            val status = conn.responseCode
            Log.d(TAG, "responseCode: $status")

            val reader = if (status == HttpURLConnection.HTTP_OK)
                BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
            else
                BufferedReader(InputStreamReader(conn.errorStream, "UTF-8"))

            responseStr = reader.useLines { it.joinToString("\n") }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.localizedMessage}")
        } finally {
            conn?.disconnect()
        }

        Log.d(TAG, "POST response = $responseStr")
        responseStr
    }
}
