package com.nanwe.nbizframemobile_webview_kotiln

import android.content.Context
import android.content.SharedPreferences

object AppPreferenceManager {

    private const val PREFERENCES_NAME = "rebuild_preference"
    private const val DEFAULT_STRING = ""
    private const val DEFAULT_BOOLEAN = false
    private const val DEFAULT_INT = -1
    private const val DEFAULT_LONG = -1L
    private const val DEFAULT_FLOAT = -1F

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        }
    }

    fun setString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    fun getString(key: String): String = prefs.getString(key, DEFAULT_STRING) ?: DEFAULT_STRING

    fun setBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun getBoolean(key: String): Boolean = prefs.getBoolean(key, DEFAULT_BOOLEAN)

    fun setInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    fun getInt(key: String): Int = prefs.getInt(key, DEFAULT_INT)

    fun setLong(key: String, value: Long) = prefs.edit().putLong(key, value).apply()
    fun getLong(key: String): Long = prefs.getLong(key, DEFAULT_LONG)

    fun setFloat(key: String, value: Float) = prefs.edit().putFloat(key, value).apply()
    fun getFloat(key: String): Float = prefs.getFloat(key, DEFAULT_FLOAT)

    fun removeKey(key: String) = prefs.edit().remove(key).apply()
    fun clear() = prefs.edit().clear().apply()
}