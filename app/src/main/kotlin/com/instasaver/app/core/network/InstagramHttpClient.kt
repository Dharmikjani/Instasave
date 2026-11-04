package com.instasaver.app.core.network

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object InstagramHttpClient {

    const val USER_AGENT_MOBILE =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4.1 Mobile/15E148 Safari/604.1"

    const val USER_AGENT_ANDROID =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8 Build/UD1A.230803.041) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36"

    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val host = url.host
            cookieStore.getOrPut(host) { mutableListOf() }.addAll(cookies)
            cookieStore.getOrPut("instagram.com") { mutableListOf() }.addAll(cookies)
            cookieStore.getOrPut("www.instagram.com") { mutableListOf() }.addAll(cookies)
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val list = mutableListOf<Cookie>()

            // 1. In-memory cookies (matching instagram.com or www.instagram.com)
            for ((domain, cookies) in cookieStore) {
                if (url.host == domain || url.host.endsWith(".$domain") || domain.endsWith(url.host)) {
                    list.addAll(cookies)
                }
            }

            // 2. Read directly from Android WebView CookieManager (Single Source of Truth)
            try {
                val cookieManager = CookieManager.getInstance()
                val cookieHeader = cookieManager.getCookie(url.toString())
                    ?: cookieManager.getCookie("https://www.instagram.com")
                    ?: cookieManager.getCookie("https://instagram.com")

                if (!cookieHeader.isNullOrBlank()) {
                    val pairs = cookieHeader.split(";")
                    for (pair in pairs) {
                        val parts = pair.trim().split("=", limit = 2)
                        if (parts.size == 2) {
                            val c = Cookie.Builder()
                                .name(parts[0].trim())
                                .value(parts[1].trim())
                                .domain(url.host)
                                .path("/")
                                .build()
                            // Avoid duplicates by name
                            if (list.none { it.name == c.name }) {
                                list.add(c)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore in non-Android/unit test environments
            }

            return list
        }
    }

    private val headersInterceptor = Interceptor { chain ->
        val original = chain.request()
        val host = original.url.host
        val requestBuilder = original.newBuilder()
            .header("User-Agent", USER_AGENT_ANDROID)
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")

        // Only attach internal Instagram API/AJAX headers and cookies to instagram.com web/api domains,
        // NEVER to static CDN domains (cdninstagram.com, fbcdn.net) as it breaks Akamai/Cloudflare caching & security.
        val isInstagramHost = (host == "instagram.com" || host.endsWith(".instagram.com")) &&
                !host.contains("cdninstagram.com")

        if (isInstagramHost) {
            requestBuilder
                .header("X-IG-App-ID", "936619743392459")
                .header("X-ASBD-ID", "129477")
                .header("X-IG-WWW-Claim", "0")
                .header("X-Requested-With", "XMLHttpRequest")

            // Auto-extract and attach X-CSRFToken and Cookie header
            try {
                val cookieManager = CookieManager.getInstance()
                val cookieString = cookieManager.getCookie("https://www.instagram.com")
                    ?: cookieManager.getCookie("https://instagram.com")

                if (!cookieString.isNullOrBlank()) {
                    requestBuilder.header("Cookie", cookieString)

                    // Find csrftoken
                    for (pair in cookieString.split(";")) {
                        val p = pair.trim().split("=", limit = 2)
                        if (p.size == 2 && p[0].trim() == "csrftoken") {
                            requestBuilder.header("X-CSRFToken", p[1].trim())
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore in tests
            }
        }

        chain.proceed(requestBuilder.build())
    }

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            .addInterceptor(headersInterceptor)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun setSessionCookies(cookiesHeader: String) {
        val pairs = cookiesHeader.split(";")
        val list = mutableListOf<Cookie>()
        for (pair in pairs) {
            val parts = pair.trim().split("=", limit = 2)
            if (parts.size == 2) {
                val name = parts[0].trim()
                val value = parts[1].trim()
                val cookie = Cookie.Builder()
                    .name(name)
                    .value(value)
                    .domain("instagram.com")
                    .path("/")
                    .build()
                list.add(cookie)
            }
        }
        cookieStore["instagram.com"] = list
        cookieStore["www.instagram.com"] = list

        // Also push to Android CookieManager
        try {
            val cm = CookieManager.getInstance()
            for (c in list) {
                cm.setCookie("https://www.instagram.com", "${c.name}=${c.value}; Domain=.instagram.com; Path=/")
            }
            cm.flush()
        } catch (e: Exception) {
            // ignore
        }
    }
}
