package com.instasaver.app.core.network

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object InstagramSessionManager {

    private const val PREFS_NAME = "instasaver_session_prefs"
    private const val KEY_COOKIES = "ig_cookies"
    private const val KEY_USERNAME = "ig_username"

    private lateinit var prefs: SharedPreferences

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _activeUsername = MutableStateFlow<String?>(null)
    val activeUsername: StateFlow<String?> = _activeUsername.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCookies = prefs.getString(KEY_COOKIES, null)
        val savedUser = prefs.getString(KEY_USERNAME, null)

        if (!savedCookies.isNullOrBlank() && savedCookies.contains("sessionid=")) {
            _isLoggedIn.value = true
            _activeUsername.value = savedUser
            InstagramHttpClient.setSessionCookies(savedCookies)
        }
    }

    fun saveSession(cookies: String, username: String? = null) {
        prefs.edit()
            .putString(KEY_COOKIES, cookies)
            .putString(KEY_USERNAME, username)
            .apply()

        _isLoggedIn.value = true
        _activeUsername.value = username
        InstagramHttpClient.setSessionCookies(cookies)
    }

    fun getCookies(): String? {
        return prefs.getString(KEY_COOKIES, null)
    }

    fun logout() {
        prefs.edit().clear().apply()
        _isLoggedIn.value = false
        _activeUsername.value = null
        try {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        } catch (e: Exception) {
            // ignore
        }
    }
}

