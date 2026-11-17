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
     * Prepares guest session if needed without injecting invalid/forged tokens.
     */
    fun ensureGuestCookies() {
        // Intentionally no-op: Instagram server-side HMAC validation rejects synthetic UUID cookies.
        // Public endpoints function better with no cookies or cookies issued by Instagram edge servers.
    }
}
