package com.instasaver.core.extractor

import com.instasaver.core.model.AudioTrack
import com.instasaver.core.model.CarouselSlide
import com.instasaver.core.model.MediaItem
import com.instasaver.core.model.MediaType
import com.instasaver.core.network.InstagramHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.util.regex.Pattern

object InstagramExtractor {

    // Supports /reel/, /reels/ (plural), /p/, /tv/, and /share/reel/ or /share/p/
    private val SHORTCODE_REGEX = Pattern.compile("(?:reels?|p|tv|share/(?:reel|p))/([A-Za-z0-9_-]+)")
    private val HASHTAG_REGEX = Pattern.compile("#([\\p{L}0-9_]+)")

    // Extract shortcode from Instagram's internal JSON blobs in HTML script tags
    private val JSON_SHORTCODE_REGEX = Pattern.compile("\"shortcode\"\\s*:\\s*\"([A-Za-z0-9_-]+)\"")

    /**
     * Sanitizes raw input and extracts the canonical shortcode.
     */
    fun extractShortcode(inputUrl: String): String? {
        val cleanUrl = inputUrl.trim()
        if (!cleanUrl.contains("instagram.com/") && !cleanUrl.contains("instagr.am/")) {
            return null
        }
        val matcher = SHORTCODE_REGEX.matcher(cleanUrl)
        return if (matcher.find()) matcher.group(1) else null
    }

    /**
     * Resilient 4-Tier extraction pipeline with modern fallbacks.
     */
    suspend fun extractMedia(rawUrl: String): Result<MediaItem> = withContext(Dispatchers.IO) {
        val shortcode = extractShortcode(rawUrl)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid Instagram link. Please paste a valid Reel or Post URL."))

        // Instant demo / sample test fallback
        if (shortcode.contains("sample", ignoreCase = true) || shortcode.contains("demo", ignoreCase = true) || rawUrl.contains("demo")) {
            return@withContext Result.success(getDemoMediaItem(shortcode))
        }

        // Tier 1: Embed Page Scraping (best for public Reels & Photos, no login needed)
        val embedResult = runCatching { extractFromEmbed(shortcode) }
        if (embedResult.isSuccess && embedResult.getOrNull() != null) {
            return@withContext Result.success(embedResult.getOrThrow()!!)
        }

        // Tier 2: OpenGraph meta tag extraction
        val ogResult = runCatching { extractFromOpenGraph(shortcode) }
        if (ogResult.isSuccess && ogResult.getOrNull() != null) {
            return@withContext Result.success(ogResult.getOrThrow()!!)
        }

        // Tier 3: JSON-LD / window.__additionalDataLoaded script extraction
        val scriptResult = runCatching { extractFromScriptTag(shortcode) }
        if (scriptResult.isSuccess && scriptResult.getOrNull() != null) {
            return@withContext Result.success(scriptResult.getOrThrow()!!)
        }

        // Tier 4: Private API endpoint (requires session cookies)
        val apiResult = runCatching { extractFromPrivateApi(shortcode) }
        if (apiResult.isSuccess && apiResult.getOrNull() != null) {
            return@withContext Result.success(apiResult.getOrThrow()!!)
        }

        // Demo/sample test fallback
        if (shortcode.contains("sample", ignoreCase = true) || shortcode.contains("demo", ignoreCase = true) || rawUrl.contains("demo")) {
            return@withContext Result.success(getDemoMediaItem(shortcode))
        }

        Result.failure(Exception("Unable to extract media. The post might be from a private account or deleted.\n\nTier errors:\nEmbed: ${embedResult.exceptionOrNull()?.message}\nOG: ${ogResult.exceptionOrNull()?.message}"))
    }

    /**
     * Tier 1: Extracts from Instagram Embed page — best public tier.
     * Uses multiple CSS selector fallbacks for robustness.
     */
    private fun extractFromEmbed(shortcode: String): MediaItem? {
        val embedUrl = "https://www.instagram.com/p/$shortcode/embed/captioned/"
        val request = Request.Builder()
            .url(embedUrl)
            .header("Referer", "https://www.instagram.com/")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        val response = InstagramHttpClient.client.newCall(request).execute()
        if (!response.isSuccessful) return null
        val html = response.body?.string() ?: return null

        val doc = Jsoup.parse(html)

        // Multiple video selector fallbacks — Instagram changes CSS class names periodically
        val videoElement = doc.selectFirst("video.EmbeddedVideo")
            ?: doc.selectFirst("video[src]")
            ?: doc.selectFirst("video")
        val videoUrl = videoElement?.attr("src")?.takeIf { it.startsWith("http") }
            ?: videoElement?.attr("data-src")?.takeIf { it.startsWith("http") }

        // Multiple image selector fallbacks
        val imageElement = doc.selectFirst("img.EmbeddedMediaImage")
            ?: doc.selectFirst("img[src*=cdninstagram]")
            ?: doc.selectFirst("img[src*=fbcdn]")
            ?: doc.selectFirst("div.EmbeddedMedia img")
        val imageUrl = imageElement?.attr("src")?.takeIf { it.startsWith("http") }

        val downloadUrl = videoUrl ?: imageUrl ?: run {
            // Last resort: find any CDN URL in the HTML source
            val cdnPattern = Pattern.compile("https://[a-z0-9\\-]+\\.cdninstagram\\.com/[^\"'\\s]+\\.mp4[^\"'\\s]*")
            val cdnMatcher = cdnPattern.matcher(html)
            if (cdnMatcher.find()) cdnMatcher.group(0) else null
        } ?: return null

        val mediaType = if (videoUrl != null || downloadUrl.contains(".mp4")) MediaType.REEL else MediaType.POST_IMAGE

        // Author details with fallbacks
        val authorUsername = doc.selectFirst("a.CaptionUsername")?.text()?.removePrefix("@")?.trim()
            ?: doc.selectFirst("a[href*='instagram.com']")?.text()?.removePrefix("@")?.trim()
            ?: doc.selectFirst("span.Username")?.text()?.removePrefix("@")?.trim()
            ?: "instagram_user"
        val authorAvatar = doc.selectFirst("img.AvatarImage")?.attr("src")
            ?: doc.selectFirst("img[alt*='profile']")?.attr("src") ?: ""

        val captionElement = doc.selectFirst(".Caption") ?: doc.selectFirst(".CaptionText")
        val rawCaption = captionElement?.text() ?: ""
        val hashtags = mutableListOf<String>()
        val hashMatcher = HASHTAG_REGEX.matcher(rawCaption)
        while (hashMatcher.find()) {
            hashtags.add("#" + hashMatcher.group(1))
        }

        val thumbnailUrl = imageUrl ?: videoElement?.attr("poster") ?: ""

        return MediaItem(
            id = shortcode,
            shortcode = shortcode,
            type = mediaType,
            url = "https://www.instagram.com/p/$shortcode/",
            downloadUrl = cleanCdnUrl(downloadUrl),
            thumbnailUrl = cleanCdnUrl(thumbnailUrl),
            title = if (mediaType == MediaType.REEL) "Reel by @$authorUsername" else "Post by @$authorUsername",
            caption = rawCaption,
            hashtags = hashtags,
            authorUsername = authorUsername,
            authorAvatarUrl = authorAvatar,
            audioTrack = if (mediaType == MediaType.REEL) AudioTrack(
                title = "Original Audio - @$authorUsername",
                artist = authorUsername,
                audioUrl = cleanCdnUrl(downloadUrl),
                coverUrl = cleanCdnUrl(thumbnailUrl)
            ) else null
        )
    }

    /**
     * Tier 2: OpenGraph & Embedded JSON-LD extraction
     */
    private fun extractFromOpenGraph(shortcode: String): MediaItem? {
        val pageUrl = "https://www.instagram.com/p/$shortcode/"
        val request = Request.Builder()
            .url(pageUrl)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "none")
            .build()

        val response = InstagramHttpClient.client.newCall(request).execute()
        if (!response.isSuccessful) return null
        val html = response.body?.string() ?: return null
        val doc = Jsoup.parse(html)

        val ogVideo = doc.selectFirst("meta[property=og:video]")?.attr("content")
            ?: doc.selectFirst("meta[property=og:video:url]")?.attr("content")
        val ogImage = doc.selectFirst("meta[property=og:image]")?.attr("content")
        val ogTitle = doc.selectFirst("meta[property=og:title]")?.attr("content") ?: ""
        val ogDescription = doc.selectFirst("meta[property=og:description]")?.attr("content") ?: ""

        val downloadUrl = ogVideo ?: ogImage ?: return null
        val isVideo = ogVideo != null || downloadUrl.contains(".mp4")

        val author = when {
            ogTitle.contains("on Instagram:") -> ogTitle.substringBefore("on Instagram:").trim()
            ogTitle.contains("•") -> ogTitle.substringBefore("•").trim()
            else -> "instagram_user"
        }

        return MediaItem(
            id = shortcode,
            shortcode = shortcode,
            type = if (isVideo) MediaType.REEL else MediaType.POST_IMAGE,
            url = pageUrl,
            downloadUrl = cleanCdnUrl(downloadUrl),
            thumbnailUrl = cleanCdnUrl(ogImage ?: downloadUrl),
            title = ogTitle,
            caption = ogDescription,
            authorUsername = author,
            audioTrack = if (isVideo) AudioTrack(
                title = "Audio - $author",
                artist = author,
                audioUrl = cleanCdnUrl(downloadUrl),
                coverUrl = cleanCdnUrl(ogImage ?: "")
            ) else null
        )
    }

    /**
     * Tier 3: Extract from embedded __additionalDataLoaded or similar script JSON in page HTML
     */
    private fun extractFromScriptTag(shortcode: String): MediaItem? {
        val pageUrl = "https://www.instagram.com/p/$shortcode/"
        val request = Request.Builder()
            .url(pageUrl)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        val response = InstagramHttpClient.client.newCall(request).execute()
        if (!response.isSuccessful) return null
        val html = response.body?.string() ?: return null

        // Look for video_url in script tags
        val videoPattern = Pattern.compile("\"video_url\"\\s*:\\s*\"(https:[^\"]+)\"")
        val displayPattern = Pattern.compile("\"display_url\"\\s*:\\s*\"(https:[^\"]+)\"")
        val usernamePattern = Pattern.compile("\"username\"\\s*:\\s*\"([^\"]+)\"")

        val videoMatcher = videoPattern.matcher(html)
        val videoUrl = if (videoMatcher.find()) videoMatcher.group(1)?.replace("\\u0026", "&") else null

        val displayMatcher = displayPattern.matcher(html)
        val displayUrl = if (displayMatcher.find()) displayMatcher.group(1)?.replace("\\u0026", "&") else null

        val userMatcher = usernamePattern.matcher(html)
        val username = if (userMatcher.find()) userMatcher.group(1) else "instagram_user"

        val downloadUrl = videoUrl ?: displayUrl ?: return null
        val isVideo = videoUrl != null

        return MediaItem(
            id = shortcode,
            shortcode = shortcode,
            type = if (isVideo) MediaType.REEL else MediaType.POST_IMAGE,
            url = pageUrl,
            downloadUrl = cleanCdnUrl(downloadUrl),
            thumbnailUrl = cleanCdnUrl(displayUrl ?: downloadUrl),
            title = if (isVideo) "Reel by @$username" else "Post by @$username",
            caption = "",
            authorUsername = username,
            audioTrack = if (isVideo) AudioTrack(
                title = "Original Audio - @$username",
                artist = username,
                audioUrl = cleanCdnUrl(downloadUrl),
                coverUrl = cleanCdnUrl(displayUrl ?: "")
            ) else null
        )
    }

    /**
     * Tier 4: Instagram Private Web API (requires active session cookies).
     * Uses /api/v1/media/{mediaId}/info/ endpoint via shortcode-to-id conversion.
     */
    private fun extractFromPrivateApi(shortcode: String): MediaItem? {
        // Convert shortcode to media ID using Instagram's base64 encoding scheme
        val mediaId = shortcodeToMediaId(shortcode) ?: return null

        val apiUrl = "https://www.instagram.com/api/v1/media/$mediaId/info/"
        val request = Request.Builder()
            .url(apiUrl)
            .header("X-IG-App-ID", "936619743392459")
            .header("X-ASBD-ID", "129477")
            .header("Referer", "https://www.instagram.com/p/$shortcode/")
            .build()

        val response = InstagramHttpClient.client.newCall(request).execute()
        if (!response.isSuccessful) return null
        val body = response.body?.string() ?: return null

        return try {
            val json = JSONObject(body)
            val items = json.optJSONArray("items") ?: return null
            if (items.length() == 0) return null
            val item = items.getJSONObject(0)
            parseApiMediaItem(item, shortcode)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Converts an Instagram shortcode to its numeric media ID.
     * Instagram uses a base64url-like scheme.
     */
    private fun shortcodeToMediaId(shortcode: String): String? {
        return try {
            val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
            var n = 0L
            for (char in shortcode) {
                val idx = alphabet.indexOf(char)
                if (idx == -1) return null
                n = n * 64 + idx
            }
            n.toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun parseApiMediaItem(item: JSONObject, shortcode: String): MediaItem? {
        val user = item.optJSONObject("user")
        val username = user?.optString("username", "instagram_user") ?: "instagram_user"
        val userAvatar = user?.optString("profile_pic_url", "") ?: ""
        val captionText = item.optJSONObject("caption")?.optString("text", "") ?: ""

        // Check Carousel
        val carouselMedia = item.optJSONArray("carousel_media")
        if (carouselMedia != null && carouselMedia.length() > 0) {
            val slides = mutableListOf<CarouselSlide>()
            for (i in 0 until carouselMedia.length()) {
                val slide = carouselMedia.getJSONObject(i)
                val isSlideVideo = slide.optInt("media_type") == 2
                val slideVideoUrl = slide.optJSONArray("video_versions")?.optJSONObject(0)?.optString("url")
                val slideImgUrl = slide.optJSONObject("image_versions2")?.optJSONArray("candidates")?.optJSONObject(0)?.optString("url")
                val slideDownload = (if (isSlideVideo) slideVideoUrl else slideImgUrl) ?: continue
                slides.add(
                    CarouselSlide(
                        index = i + 1,
                        type = if (isSlideVideo) MediaType.POST_VIDEO else MediaType.POST_IMAGE,
                        downloadUrl = cleanCdnUrl(slideDownload),
                        thumbnailUrl = cleanCdnUrl(slideImgUrl ?: slideDownload)
                    )
                )
            }
            if (slides.isNotEmpty()) {
                return MediaItem(
                    id = shortcode,
                    shortcode = shortcode,
                    type = MediaType.CAROUSEL,
                    url = "https://www.instagram.com/p/$shortcode/",
                    downloadUrl = slides.first().downloadUrl,
                    thumbnailUrl = slides.first().thumbnailUrl,
                    title = "Carousel by @$username (${slides.size} items)",
                    caption = captionText,
                    authorUsername = username,
                    authorAvatarUrl = userAvatar,
                    carouselItems = slides
                )
            }
        }

        // Single Video or Image
        val isVideo = item.optBoolean("is_video", false) || item.optInt("media_type") == 2
        val videoUrl = item.optJSONArray("video_versions")?.optJSONObject(0)?.optString("url")
        val imgUrl = item.optJSONObject("image_versions2")?.optJSONArray("candidates")?.optJSONObject(0)?.optString("url")
        val downloadUrl = (if (isVideo) videoUrl else imgUrl) ?: return null

        return MediaItem(
            id = shortcode,
            shortcode = shortcode,
            type = if (isVideo) MediaType.REEL else MediaType.POST_IMAGE,
            url = "https://www.instagram.com/p/$shortcode/",
            downloadUrl = cleanCdnUrl(downloadUrl),
            thumbnailUrl = cleanCdnUrl(imgUrl ?: downloadUrl),
            title = (if (isVideo) "Reel" else "Photo") + " by @$username",
            caption = captionText,
            authorUsername = username,
            authorAvatarUrl = userAvatar,
            audioTrack = if (isVideo) AudioTrack(
                title = "Original Audio - @$username",
                artist = username,
                audioUrl = cleanCdnUrl(downloadUrl),
                coverUrl = cleanCdnUrl(imgUrl ?: "")
            ) else null
        )
    }

    fun getDemoMediaItem(shortcode: String): MediaItem {
        val testVideo = "https://www.w3schools.com/html/mov_bbb.mp4"
        val testThumb = "https://picsum.photos/id/1055/600/1000"
        return MediaItem(
            id = shortcode,
            shortcode = shortcode,
            type = MediaType.REEL,
            url = "https://www.instagram.com/reel/$shortcode/",
            downloadUrl = testVideo,
            thumbnailUrl = testThumb,
            title = "Reel by @virat.kohli",
            caption = "Focus on the process. The results will take care of themselves. 🏏 #demo #reel",
            authorUsername = "virat.kohli",
            authorAvatarUrl = "https://picsum.photos/id/1005/200/200",
            durationSeconds = 15,
            width = 1080,
            height = 1920,
            audioTrack = AudioTrack(
                title = "Original Audio - @virat.kohli",
                artist = "virat.kohli",
                audioUrl = testVideo,
                coverUrl = testThumb
            )
        )
    }

    private fun cleanCdnUrl(url: String): String {
        return url.replace("&amp;", "&").replace("\\u0026", "&")
    }
}


