package com.instasaver.app.core.extractor

import android.webkit.CookieManager
import com.instasaver.app.core.model.MediaItem
import com.instasaver.app.core.model.MediaType
import com.instasaver.app.core.model.ProfileData
import com.instasaver.app.core.network.InstagramHttpClient
import com.instasaver.app.core.network.InstagramSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.regex.Pattern

object ProfileScraper {

    private const val IG_APP_ID = "936619743392459"
    private const val ASBD_ID = "129477"

    private val STATS_PATTERN = Pattern.compile("([0-9.,]+[KMBkmb]?)\\s*Followers,\\s*([0-9.,]+[KMBkmb]?)\\s*Following,\\s*([0-9.,]+)\\s*Posts", Pattern.CASE_INSENSITIVE)

    suspend fun getProfile(usernameInput: String): Result<ProfileData> = withContext(Dispatchers.IO) {
        val cleanUsername = usernameInput.trim().removePrefix("@")
        if (cleanUsername.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid Instagram username"))
        }

        // Method 1: Web Profile Info API — always try first for accurate stats
        val apiResult = runCatching { fetchWebProfileInfo(cleanUsername) }
        val apiProfile = if (apiResult.isSuccess) apiResult.getOrNull() else null

        // If API gave us content (reels or posts), return immediately
        if (apiProfile != null && (apiProfile.reels.isNotEmpty() || apiProfile.posts.isNotEmpty() || apiProfile.isPrivate)) {
            return@withContext Result.success(apiProfile)
        }

        // Method 2: HTML Page + Meta Description Stats + Embedded Script Parsing
        val htmlResult = runCatching { fetchProfileFromHtmlWithScraping(cleanUsername) }
        val htmlProfile = if (htmlResult.isSuccess) htmlResult.getOrNull() else null

        return@withContext when {
            // Both methods succeeded — merge: prefer API stats, HTML content as fallback
            apiProfile != null && htmlProfile != null -> {
                Result.success(
                    htmlProfile.copy(
                        fullName = apiProfile.fullName.ifEmpty { htmlProfile.fullName },
                        hdAvatarUrl = apiProfile.hdAvatarUrl.ifEmpty { htmlProfile.hdAvatarUrl },
                        isVerified = apiProfile.isVerified || htmlProfile.isVerified,
                        isPrivate = apiProfile.isPrivate,
                        // CRITICAL: Always use API stats if available (they are accurate integers)
                        followersCount = if (apiProfile.followersCount.isNotEmpty() && apiProfile.followersCount != "0") apiProfile.followersCount else htmlProfile.followersCount,
                        followingCount = if (apiProfile.followingCount.isNotEmpty() && apiProfile.followingCount != "0") apiProfile.followingCount else htmlProfile.followingCount,
                        postsCount = if (apiProfile.postsCount > 0) apiProfile.postsCount else htmlProfile.postsCount,
                        reels = if (apiProfile.reels.isNotEmpty()) apiProfile.reels else htmlProfile.reels,
                        posts = if (apiProfile.posts.isNotEmpty()) apiProfile.posts else htmlProfile.posts
                    )
                )
            }
            // Only API succeeded
            apiProfile != null -> Result.success(apiProfile)
            // Only HTML succeeded
            htmlProfile != null -> Result.success(htmlProfile)
            // Both failed
            else -> Result.failure(Exception("Unable to load profile for @$cleanUsername. The account may be private or Instagram is currently limiting access."))
        }
    }

    private fun fetchWebProfileInfo(username: String): ProfileData? {
        val url = "https://www.instagram.com/api/v1/users/web_profile_info/?username=$username"
        val requestBuilder = Request.Builder()
            .url(url)
            .header("X-IG-App-ID", IG_APP_ID)
            .header("X-ASBD-ID", ASBD_ID)
            .header("X-IG-WWW-Claim", "0")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Referer", "https://www.instagram.com/$username/")

        // Attach WebView cookies directly
        try {
            val cm = CookieManager.getInstance()
            val cookies = cm.getCookie("https://www.instagram.com")
                ?: InstagramSessionManager.getCookies()

            if (!cookies.isNullOrBlank()) {
                requestBuilder.header("Cookie", cookies)
                for (pair in cookies.split(";")) {
                    val p = pair.trim().split("=", limit = 2)
                    if (p.size == 2 && p[0].trim() == "csrftoken") {
                        requestBuilder.header("X-CSRFToken", p[1].trim())
                        break
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }

        val response = InstagramHttpClient.client.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            return fetchMobileApiProfile(username)
        }
        val body = response.body?.string() ?: return fetchMobileApiProfile(username)

        val json = if (body.startsWith("{")) JSONObject(body) else return fetchMobileApiProfile(username)
        val data = json.optJSONObject("data") ?: return fetchMobileApiProfile(username)
        val user = data.optJSONObject("user") ?: return fetchMobileApiProfile(username)

        val fullName = user.optString("full_name", username)
        val bio = user.optString("biography", "")
        val hdAvatar = user.optJSONObject("hd_profile_pic_url_info")?.optString("url")
            ?: user.optString("profile_pic_url_hd")
            ?: user.optString("profile_pic_url", "")
        val isPrivate = user.optBoolean("is_private", false)
        val isVerified = user.optBoolean("is_verified", false)
        val externalUrl = user.optString("external_url", "")

        val followerCountInt = user.optJSONObject("edge_followed_by")?.optInt("count", 0) ?: 0
        val followingCountInt = user.optJSONObject("edge_follow")?.optInt("count", 0) ?: 0
        val postsCountInt = user.optJSONObject("edge_owner_to_timeline_media")?.optInt("count", 0) ?: 0

        val reelsMap = LinkedHashMap<String, MediaItem>()
        val postsMap = LinkedHashMap<String, MediaItem>()

        // 1. Parse REELS (edge_clips_tab or legacy edge_felix_video_timeline)
        val clipsTabEdges = user.optJSONObject("edge_clips_tab")?.optJSONArray("edges")
            ?: user.optJSONObject("edge_felix_video_timeline")?.optJSONArray("edges")
        if (clipsTabEdges != null) {
            for (i in 0 until clipsTabEdges.length()) {
                val node = clipsTabEdges.getJSONObject(i).optJSONObject("node") ?: continue
                val item = parseNodeToMedia(node, username, hdAvatar, forceVideo = true)
                if (item.shortcode.isNotEmpty()) {
                    reelsMap[item.shortcode] = item
                }
            }
        }

        // 2. Parse Timeline Media (edge_owner_to_timeline_media)
        val timelineEdges = user.optJSONObject("edge_owner_to_timeline_media")?.optJSONArray("edges")
        if (timelineEdges != null) {
            for (i in 0 until timelineEdges.length()) {
                val node = timelineEdges.getJSONObject(i).optJSONObject("node") ?: continue
                val isVideo = node.optBoolean("is_video", false) || node.optJSONObject("media")?.optBoolean("is_video", false) == true
                val item = parseNodeToMedia(node, username, hdAvatar, forceVideo = isVideo)
                if (item.shortcode.isEmpty()) continue

                if (isVideo) {
                    if (!reelsMap.containsKey(item.shortcode)) {
                        reelsMap[item.shortcode] = item
                    }
                } else {
                    postsMap[item.shortcode] = item
                }
            }
        }

        return ProfileData(
            username = username,
            fullName = fullName,
            bio = bio,
            hdAvatarUrl = cleanUrl(hdAvatar),
            isPrivate = isPrivate,
            isVerified = isVerified,
            postsCount = if (postsCountInt > 0) postsCountInt else (reelsMap.size + postsMap.size),
            followersCount = formatCount(followerCountInt),
            followingCount = formatCount(followingCountInt),
            externalUrl = externalUrl,
            reels = reelsMap.values.toList(),
            posts = postsMap.values.toList()
        )
    }

    private fun fetchMobileApiProfile(username: String): ProfileData? {
        val url = "https://www.instagram.com/$username/?__a=1&__d=dis"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Instagram 275.0.0.27.98 Android (33/13; 420dpi; 1080x2400; Google; Pixel 7; panther; tensor; en_US; 458229204)")
            .header("Accept", "application/json")
            .header("X-IG-App-ID", IG_APP_ID)
            .build()
        return try {
            val response = InstagramHttpClient.client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            if (!body.startsWith("{")) return null
            val json = JSONObject(body)
            val user = json.optJSONObject("graphql")?.optJSONObject("user")
                ?: json.optJSONObject("data")?.optJSONObject("user")
                ?: return null
            val followers = user.optJSONObject("edge_followed_by")?.optInt("count", 0) ?: 0
            val following = user.optJSONObject("edge_follow")?.optInt("count", 0) ?: 0
            val posts = user.optJSONObject("edge_owner_to_timeline_media")?.optInt("count", 0) ?: 0
            ProfileData(
                username = username,
                fullName = user.optString("full_name", username),
                bio = user.optString("biography", ""),
                hdAvatarUrl = user.optString("profile_pic_url_hd", user.optString("profile_pic_url", "")),
                isPrivate = user.optBoolean("is_private", false),
                isVerified = user.optBoolean("is_verified", false),
                postsCount = posts,
                followersCount = formatCount(followers),
                followingCount = formatCount(following),
                externalUrl = user.optString("external_url", ""),
                reels = emptyList(),
                posts = emptyList()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseNodeToMedia(node: JSONObject, username: String, hdAvatar: String, forceVideo: Boolean): MediaItem {
        val targetNode = node.optJSONObject("media") ?: node
        val shortcode = targetNode.optString("shortcode", targetNode.optString("code", ""))

        // Video versions candidates for full 1080p
        val videoVersions = targetNode.optJSONArray("video_versions")
        val videoUrl = videoVersions?.optJSONObject(0)?.optString("url")
            ?: targetNode.optString("video_url")

        // Image candidates for display / thumbnail
        val imageCandidates = targetNode.optJSONObject("image_versions2")?.optJSONArray("candidates")
        val displayUrl = imageCandidates?.optJSONObject(0)?.optString("url")
            ?: targetNode.optString("display_url", "")

        val caption = targetNode.optJSONObject("edge_media_to_caption")
            ?.optJSONArray("edges")
            ?.optJSONObject(0)
            ?.optJSONObject("node")
            ?.optString("text", "")
            ?: targetNode.optJSONObject("caption")?.optString("text", "")
            ?: ""

        val effectiveDownload = if (forceVideo && videoUrl.isNotEmpty()) videoUrl else displayUrl

        return MediaItem(
            id = shortcode,
            shortcode = shortcode,
            type = if (forceVideo) MediaType.REEL else MediaType.POST_IMAGE,
            url = "https://www.instagram.com/p/$shortcode/",
            downloadUrl = cleanUrl(effectiveDownload),
            thumbnailUrl = cleanUrl(displayUrl.ifEmpty { effectiveDownload }),
            title = (if (forceVideo) "Reel" else "Post") + " by @$username",
            caption = caption,
            authorUsername = username,
            authorAvatarUrl = hdAvatar
        )
    }

    /**
     * Deep HTML parser that extracts stats and embedded media from HTML & Meta-tags
     */
    private fun fetchProfileFromHtmlWithScraping(username: String): ProfileData? {
        val pageUrl = "https://www.instagram.com/$username/"
        val requestBuilder = Request.Builder().url(pageUrl)

        try {
            val cm = CookieManager.getInstance()
            val cookies = cm.getCookie("https://www.instagram.com")
            if (!cookies.isNullOrBlank()) {
                requestBuilder.header("Cookie", cookies)
            }
        } catch (e: Exception) {
            // ignore
        }

        val response = InstagramHttpClient.client.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) return null
        val html = response.body?.string() ?: return null

        val doc = Jsoup.parse(html)
        val ogTitle = doc.selectFirst("meta[property=og:title]")?.attr("content") ?: username
        val ogDesc = doc.selectFirst("meta[property=og:description]")?.attr("content") ?: ""
        val ogImage = doc.selectFirst("meta[property=og:image]")?.attr("content") ?: ""

        // Extract accurate follower, following, and post counts from og:description
        // e.g., "271M Followers, 279 Following, 1,058 Posts - See Instagram photos and videos..."
        var followersFormatted = ""
        var followingFormatted = ""
        var postsCount = 0
        var cleanBio = ""

        // Normalize non-breaking spaces (\u00A0), narrow NBSP (\u202F) & HTML entities
        val normalizedDesc = ogDesc
            .replace('\u00A0', ' ')
            .replace('\u202F', ' ')
            .replace("&nbsp;", " ")
            .trim()

        val statsMatcher = STATS_PATTERN.matcher(normalizedDesc)
        if (statsMatcher.find()) {
            followersFormatted = statsMatcher.group(1) ?: ""
            followingFormatted = statsMatcher.group(2) ?: ""
            postsCount = statsMatcher.group(3)?.replace(",", "")?.toIntOrNull() ?: 0
        } else {
            // Independent fallback extractors if order differs
            val folM = Pattern.compile("([0-9.,]+[KMBkmb]?)\\s*Followers", Pattern.CASE_INSENSITIVE).matcher(normalizedDesc)
            if (folM.find()) followersFormatted = folM.group(1) ?: ""
            val fngM = Pattern.compile("([0-9.,]+[KMBkmb]?)\\s*Following", Pattern.CASE_INSENSITIVE).matcher(normalizedDesc)
            if (fngM.find()) followingFormatted = fngM.group(1) ?: ""
            val pstM = Pattern.compile("([0-9.,]+)\\s*Posts", Pattern.CASE_INSENSITIVE).matcher(normalizedDesc)
            if (pstM.find()) postsCount = pstM.group(1)?.replace(",", "")?.toIntOrNull() ?: 0
        }

        // Extract real bio from title or description
        cleanBio = if (normalizedDesc.contains("photos and videos from")) {
            val afterBrackets = normalizedDesc.substringAfter("):", "")
            if (afterBrackets.isNotEmpty()) afterBrackets.trim() else normalizedDesc
        } else {
            normalizedDesc
        }

        val fullName = if (ogTitle.contains("(@")) {
            ogTitle.substringBefore("(@").trim()
        } else {
            ogTitle.substringBefore("•").trim()
        }

        val reelsMap = LinkedHashMap<String, MediaItem>()
        val postsMap = LinkedHashMap<String, MediaItem>()

        // Scan script tags for shortcodes & media items atomically within the same JSON block
        val mediaBlockPattern = Pattern.compile("\"shortcode\":\\s*\"([A-Za-z0-9_-]+)\"[^\\n\\r]{1,1000}?\"display_url\":\\s*\"(https:[^\"\\\\]+)\"")

        val scripts = doc.select("script")
        for (script in scripts) {
            val content = script.html()
            if (content.contains("\"shortcode\":") && content.contains("\"display_url\":")) {
                val blockMatcher = mediaBlockPattern.matcher(content)
                while (blockMatcher.find()) {
                    val code = blockMatcher.group(1) ?: continue
                    if (reelsMap.containsKey(code) || postsMap.containsKey(code)) continue

                    val matchedBlock = blockMatcher.group(0) ?: ""
                    val isVid = matchedBlock.contains("\"is_video\":true") || matchedBlock.contains("\"is_video\": true")
                    val rawUrl = blockMatcher.group(2) ?: ""
                    val dispUrl = cleanUrl(rawUrl.replace("\\u0026", "&"))

                    val item = MediaItem(
                        id = code,
                        shortcode = code,
                        type = if (isVid) MediaType.REEL else MediaType.POST_IMAGE,
                        url = "https://www.instagram.com/p/$code/",
                        downloadUrl = dispUrl,
                        thumbnailUrl = dispUrl,
                        title = (if (isVid) "Reel" else "Post") + " by @$username",
                        caption = "",
                        authorUsername = username,
                        authorAvatarUrl = ogImage
                    )

                    if (isVid) reelsMap[code] = item else postsMap[code] = item
                }
            }
        }

        return ProfileData(
            username = username,
            fullName = fullName.ifEmpty { username },
            bio = cleanBio,
            hdAvatarUrl = cleanUrl(ogImage),
            isPrivate = false,
            isVerified = false,
            postsCount = if (postsCount > 0) postsCount else (reelsMap.size + postsMap.size),
            followersCount = followersFormatted,
            followingCount = followingFormatted,
            externalUrl = "",
            reels = reelsMap.values.toList(),
            posts = postsMap.values.toList()
        )
    }

    fun formatCount(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }

    private fun cleanUrl(url: String): String =
        url.replace("&amp;", "&").replace("\\u0026", "&")
}
