package com.instasaver.core.network

import android.webkit.CookieManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstagramHeadersInterceptor @Inject constructor() : Interceptor {

    companion object {
        const val USER_AGENT_MOBILE =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4.1 Mobile/15E148 Safari/604.1"

        const val USER_AGENT_ANDROID =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8 Build/UD1A.230803.041) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36"

        const val USER_AGENT_DESKTOP =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val host = original.url.host
        val originalUa = original.header("User-Agent")
        val effectiveUa = if (!originalUa.isNullOrBlank()) originalUa else USER_AGENT_ANDROID
        val requestBuilder = original.newBuilder()
            .header("User-Agent", effectiveUa)
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

        return chain.proceed(requestBuilder.build())
    }
}
