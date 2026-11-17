package com.instasaver.core.network

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstagramCookieJar @Inject constructor() : CookieJar {

    private val cookieStore = ConcurrentHashMap<String, CopyOnWriteArrayList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        cookieStore.getOrPut(host) { CopyOnWriteArrayList() }.addAll(cookies)
        cookieStore.getOrPut("instagram.com") { CopyOnWriteArrayList() }.addAll(cookies)
        cookieStore.getOrPut("www.instagram.com") { CopyOnWriteArrayList() }.addAll(cookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val list = mutableListOf<Cookie>()

        // 1. In-memory cookies (matching instagram.com or www.instagram.com)
        val host = url.host
        for ((domain, cookies) in cookieStore) {
            if (host == domain || host.endsWith(".$domain")) {
                list.addAll(cookies)
            }
        }

        // 2. Read directly from Android WebView CookieManager (Single Source of Truth) ONLY for Instagram domains
        val isInstagram = host == "instagram.com" || host.endsWith(".instagram.com")
        if (isInstagram) {
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
        }

        return list
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
        cookieStore["instagram.com"] = CopyOnWriteArrayList(list)
        cookieStore["www.instagram.com"] = CopyOnWriteArrayList(list)

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
