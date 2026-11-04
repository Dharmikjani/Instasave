package com.instasaver.app.core.extractor

import com.instasaver.app.core.model.AudioTrack
import com.instasaver.app.core.model.CarouselSlide
import com.instasaver.app.core.model.MediaItem
import com.instasaver.app.core.model.MediaType
import com.instasaver.app.core.network.InstagramHttpClient
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
     * Resilient 3-Tier extraction pipeline.
     */
    suspend fun extractMedia(rawUrl: String): Result<MediaItem> = withContext(Dispatchers.IO) {
        val shortcode = extractShortcode(rawUrl)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid Instagram link. Please paste a valid Reel or Post URL."))

        // Tier 1: JSON Endpoint Scraper (supports complete Carousel multi-slide albums, 1080p, audio)
        val jsonResult = runCatching { extractFromJsonEndpoint(shortcode) }
        if (jsonResult.isSuccess && jsonResult.getOrNull() != null) {
            return@withContext Result.success(jsonResult.getOrThrow()!!)
        }

        // Tier 2: Embed Page Scraping (fast, login-free for single Reels & Photos)
        val embedResult = runCatching { extractFromEmbed(shortcode) }
        if (embedResult.isSuccess && embedResult.getOrNull() != null) {
            return@withContext Result.success(embedResult.getOrThrow()!!)
        }

        // Tier 3: Direct OpenGraph & JSON-LD Scraper
        val ogResult = runCatching { extractFromOpenGraph(shortcode) }
        if (ogResult.isSuccess && ogResult.getOrNull() != null) {
            return@withContext Result.success(ogResult.getOrThrow()!!)
        }

        Result.failure(Exception("Unable to extract media. The post might be from a private account or deleted."))
    }

    /**
     * Tier 1: Extracts direct CDN links from the Instagram Embed page.
     */
    private fun extractFromEmbed(shortcode: String): MediaItem? {
        val embedUrl = "https://www.instagram.com/p/$shortcode/embed/captioned/"
        val request = Request.Builder()
            .url(embedUrl)
            .header("Referer", "https://www.instagram.com/")
            .build()

        val response = InstagramHttpClient.client.newCall(request).execute()
        if (!response.isSuccessful) return null
        val html = response.body?.string() ?: return null

        val doc = Jsoup.parse(html)

        // Find video or photo
        val videoElement = doc.selectFirst("video.EmbeddedVideo")
        val videoUrl = videoElement?.attr("src")?.takeIf { it.isNotEmpty() }

        val imageElement = doc.selectFirst("img.EmbeddedMediaImage")
        val imageUrl = imageElement?.attr("src")?.takeIf { it.isNotEmpty() }

        val downloadUrl = videoUrl ?: imageUrl ?: return null
        val mediaType = if (videoUrl != null) MediaType.REEL else MediaType.POST_IMAGE

        // Author details
        val usernameElement = doc.selectFirst("a.CaptionUsername, a.Avatar")
        val authorUsername = usernameElement?.text()?.removePrefix("@")?.trim() ?: "instagram_user"
        val authorAvatar = doc.selectFirst("img.AvatarImage")?.attr("src") ?: ""

        // Caption & Hashtags
        val captionElement = doc.selectFirst(".Caption")
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
            title = "Reel by @$authorUsername",
            caption = rawCaption,
            hashtags = hashtags,
            authorUsername = authorUsername,
            authorAvatarUrl = authorAvatar,
            audioTrack = AudioTrack(
                title = "Original Audio - @$authorUsername",
                artist = authorUsername,
                audioUrl = cleanCdnUrl(downloadUrl),
                coverUrl = cleanCdnUrl(thumbnailUrl)
            )
        )
    }

    /**
     * Tier 2: OpenGraph & Embedded JSON-LD extraction
     */
    private fun extractFromOpenGraph(shortcode: String): MediaItem? {
        val pageUrl = "https://www.instagram.com/p/$shortcode/"
        val request = Request.Builder()
            .url(pageUrl)
            .build()

        val response = InstagramHttpClient.client.newCall(request).execute()
        if (!response.isSuccessful) return null
        val html = response.body?.string() ?: return null
        val doc = Jsoup.parse(html)

        val ogVideo = doc.selectFirst("meta[property=og:video]")?.attr("content")
        val ogImage = doc.selectFirst("meta[property=og:image]")?.attr("content")
        val ogTitle = doc.selectFirst("meta[property=og:title]")?.attr("content") ?: ""
        val ogDescription = doc.selectFirst("meta[property=og:description]")?.attr("content") ?: ""

        val downloadUrl = ogVideo ?: ogImage ?: return null
        val isVideo = ogVideo != null

        val author = if (ogTitle.contains("on Instagram:")) {
            ogTitle.substringBefore("on Instagram:").trim()
        } else "instagram_user"

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
            audioTrack = AudioTrack(
                title = "Audio - $author",
                artist = author,
                audioUrl = cleanCdnUrl(downloadUrl),
                coverUrl = cleanCdnUrl(ogImage ?: "")
            )
        )
    }

    /**
     * Tier 3: Querying the Instagram Web __d=dis JSON endpoint
     */
    private fun extractFromJsonEndpoint(shortcode: String): MediaItem? {
        val jsonUrl = "https://www.instagram.com/p/$shortcode/?__a=1&__d=dis"
        val request = Request.Builder()
            .url(jsonUrl)
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Referer", "https://www.instagram.com/")
            .build()

        val response = InstagramHttpClient.client.newCall(request).execute()
        if (!response.isSuccessful) return null
        val body = response.body?.string() ?: return null

        val json = JSONObject(body)
        val items = json.optJSONArray("items") ?: return null
        if (items.length() == 0) return null
        val item = items.getJSONObject(0)

        val user = item.optJSONObject("user")
        val username = user?.optString("username", "instagram_user") ?: "instagram_user"
        val userAvatar = user?.optString("profile_pic_url", "") ?: ""

        val captionObj = item.optJSONObject("caption")
        val captionText = captionObj?.optString("text", "") ?: ""

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
            authorAvatarUrl = userAvatar
        )
    }

    private fun cleanCdnUrl(url: String): String {
        return url.replace("&amp;", "&").replace("\\u0026", "&")
    }
}

