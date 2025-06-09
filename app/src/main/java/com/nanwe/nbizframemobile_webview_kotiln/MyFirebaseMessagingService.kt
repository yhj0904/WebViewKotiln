package com.nanwe.nbizframemobile_webview_kotiln

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nanwe.nbizframemobile_webview_kotiln.api.RetrofitClient
import com.nanwe.nbizframemobile_webview_kotiln.model.TokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "SPush"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")

        val userId = AppPreferenceManager.getString("userId")
        val deviceId = "ANDROID"

        val authHeader = getBearerHeader()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.push.registerToken(
                    authHeader,
                    TokenRequest(AppConstants.APP_ID, userId, deviceId, token)
                )
                if (response.isSuccessful) {
                    Log.d(TAG, "토큰 등록 성공 (FCM)")
                } else {
                    Log.e(TAG, "토큰 등록 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Retrofit 오류: ${e.localizedMessage}")
            }
        }
    }
    private fun getBearerHeader(): String? {
        val masterKey = MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        val prefs = EncryptedSharedPreferences.create(
            this,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val token = prefs.getString("access", "") ?: ""
        return if (token.isNotEmpty()) "Bearer $token" else null
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val notification = remoteMessage.notification ?: return

        wakeDevice()
        sendNotification(remoteMessage)
    }

    private fun wakeDevice() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "NBizFrameMobile::WAKELOCK"
        )
        wakeLock.acquire(3000L)
    }

    private fun getBitmapFromUrl(imageUrl: String): Bitmap? {
        return try {
            val url = URL(imageUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.doInput = true
            conn.connect()
            val inputStream = conn.inputStream
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }

    private fun sendNotification(remoteMessage: RemoteMessage) {
        val notification = remoteMessage.notification ?: return
        val data = remoteMessage.data

        val title = notification.title ?: "알림"
        val body = notification.body ?: ""
        val imageUrl = notification.imageUrl?.toString()
        val bitmap = imageUrl?.let { getBitmapFromUrl(it) }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AppConstants.PUSH_CHANNEL_ID,
                AppConstants.PUSH_CHANNEL_NM,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = AppConstants.PUSH_CHANNEL_DES
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            data["appId"]?.let { putExtra("appId", it) }
            data["noticeNo"]?.let { putExtra("noticeNo", it) }
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val defaultSoundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(this, AppConstants.PUSH_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(1000, 1000))
            .setLights(Color.BLUE, 1, 1)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (bitmap != null) {
            builder.setLargeIcon(bitmap)
            builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        notificationManager.notify(0, builder.build())
    }
}
