package com.instasaver.core.network

import android.webkit.CookieManager
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import java.util.UUID
import java.util.concurrent.TimeUnit

object InstagramHttpClient {

    const val USER_AGENT_MOBILE = InstagramHeadersInterceptor.USER_AGENT_MOBILE
    const val USER_AGENT_ANDROID = InstagramHeadersInterceptor.USER_AGENT_ANDROID
    const val USER_AGENT_DESKTOP = InstagramHeadersInterceptor.USER_AGENT_DESKTOP

    val defaultCookieJar = InstagramCookieJar()
    val defaultHeadersInterceptor = InstagramHeadersInterceptor()

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .cookieJar(defaultCookieJar)
            .addInterceptor(defaultHeadersInterceptor)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun setSessionCookies(cookiesHeader: String) {
        defaultCookieJar.setSessionCookies(cookiesHeader)
    }

    /**
     * Initializes anonymous guest session cookies so that public Instagram Web endpoints
     * (like web_profile_info and embed) accept unauthenticated requests without throwing 401.
     */
    fun ensureGuestCookies() {
        try {
            val cm = CookieManager.getInstance()
            val existing = cm.getCookie("https://www.instagram.com")
            if (existing.isNullOrBlank() || !existing.contains("csrftoken=")) {
                val guestCsrf = UUID.randomUUID().toString().replace("-", "")
                val guestMid = "Z" + UUID.randomUUID().toString().replace("-", "").take(25)
                val guestDid = UUID.randomUUID().toString()
                cm.setCookie("https://www.instagram.com", "csrftoken=$guestCsrf; Domain=.instagram.com; Path=/; Secure")
                cm.setCookie("https://www.instagram.com", "mid=$guestMid; Domain=.instagram.com; Path=/; Secure")
                cm.setCookie("https://www.instagram.com", "ig_did=$guestDid; Domain=.instagram.com; Path=/; Secure")
                cm.flush()
            }
        } catch (e: Exception) {
            // ignore in tests
        }
    }
}
