package com.instasaver.core.extractor

import android.util.Log
import android.webkit.CookieManager
import com.instasaver.core.model.MediaItem
import com.instasaver.core.model.MediaType
import com.instasaver.core.model.ProfileData
import com.instasaver.core.network.InstagramHttpClient
import com.instasaver.core.network.InstagramSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.regex.Pattern

object ProfileScraper {

    private const val TAG = "ProfileScraper"
    private const val IG_APP_ID = "936619743392459"
    private const val ASBD_ID = "129477"

    private val STATS_PATTERN = Pattern.compile("([0-9.,]+[KMBkmb]?)\\s*Followers,\\s*([0-9.,]+[KMBkmb]?)\\s*Following,\\s*([0-9.,]+)\\s*Posts", Pattern.CASE_INSENSITIVE)

    fun sanitizeUsername(input: String): String {
        var clean = input.trim()
        if (clean.contains("instagram.com/")) {
            clean = clean.substringAfter("instagram.com/")
        } else if (clean.contains("instagr.am/")) {
            clean = clean.substringAfter("instagr.am/")
        }
        clean = clean.substringBefore("?")
        clean = clean.substringBefore("/")
        return clean.removePrefix("@").trim().lowercase()
    }

    suspend fun getProfile(usernameInput: String): Result<ProfileData> = withContext(Dispatchers.IO) {
        val cleanUsername = sanitizeUsername(usernameInput)
        if (cleanUsername.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid Instagram username"))
        }

        Log.d(TAG, "Starting resilient profile scrape for @$cleanUsername")

        // 1. Initialize anonymous guest cookies if needed
        InstagramHttpClient.ensureGuestCookies()

        // 2. Tier 1: Instagram Official Web Profile Info API (with Desktop Chrome headers)
        val apiResult = runCatching { fetchWebProfileInfo(cleanUsername) }
        val apiProfile = if (apiResult.isSuccess) apiResult.getOrNull() else null
        if (apiProfile != null) {
            Log.d(TAG, "Tier 1 WebProfileInfo returned: posts=${apiProfile.posts.size}, reels=${apiProfile.reels.size}, followers=${apiProfile.followersCount}")
        } else {
            Log.d(TAG, "Tier 1 WebProfileInfo failed: ${apiResult.exceptionOrNull()?.message}")
        }

        // 3. Tier 2: Public Mirrors (Greatfon / Dumpor / Picuki / Imginn / Pixwox / StoriesIG / AnonyIG)
        val mirrorResult = runCatching { fetchProfileFromMirror(cleanUsername) }
        val mirrorProfile = if (mirrorResult.isSuccess) mirrorResult.getOrNull() else null
        if (mirrorProfile != null) {
            Log.d(TAG, "Tier 2 Mirror returned: posts=${mirrorProfile.posts.size}, reels=${mirrorProfile.reels.size}, bio=${mirrorProfile.bio.take(30)}")
        } else {
            Log.d(TAG, "Tier 2 Mirror failed: ${mirrorResult.exceptionOrNull()?.message}")
        }

        // 4. Tier 3: HTML Page + Meta Description Stats + Embedded Script Parsing
        val htmlResult = runCatching { fetchProfileFromHtmlWithScraping(cleanUsername) }
        val htmlProfile = if (htmlResult.isSuccess) htmlResult.getOrNull() else null
        if (htmlProfile != null) {
            Log.d(TAG, "Tier 3 HTML returned: posts=${htmlProfile.posts.size}, reels=${htmlProfile.reels.size}, followers=${htmlProfile.followersCount}")
        } else {
            Log.d(TAG, "Tier 3 HTML failed: ${htmlResult.exceptionOrNull()?.message}")
        }

        // Intelligently merge results across all tiers, deduplicating by ID or shortcode
        val mergedPosts = (apiProfile?.posts.orEmpty() + mirrorProfile?.posts.orEmpty() + htmlProfile?.posts.orEmpty()).distinctBy { it.id.ifEmpty { it.shortcode } }
        val mergedReels = (apiProfile?.reels.orEmpty() + mirrorProfile?.reels.orEmpty() + htmlProfile?.reels.orEmpty()).distinctBy { it.id.ifEmpty { it.shortcode } }

        val bestFullName = apiProfile?.fullName?.takeIf { it.isNotBlank() && it != cleanUsername }
            ?: htmlProfile?.fullName?.takeIf { it.isNotBlank() && it != cleanUsername }
            ?: mirrorProfile?.fullName?.takeIf { it.isNotBlank() && it != cleanUsername }
            ?: cleanUsername

        val bestAvatar = apiProfile?.hdAvatarUrl?.takeIf { it.isNotBlank() }
            ?: htmlProfile?.hdAvatarUrl?.takeIf { it.isNotBlank() }
            ?: mirrorProfile?.hdAvatarUrl?.takeIf { it.isNotBlank() }
            ?: ""

        val bestBio = mirrorProfile?.bio?.takeIf { it.isNotBlank() }
            ?: apiProfile?.bio?.takeIf { it.isNotBlank() }
            ?: htmlProfile?.bio?.takeIf { it.isNotBlank() }
            ?: ""

        val bestFollowers = apiProfile?.followersCount?.takeIf { it.isNotBlank() && it != "0" }
            ?: htmlProfile?.followersCount?.takeIf { it.isNotBlank() && it != "0" }
            ?: mirrorProfile?.followersCount?.takeIf { it.isNotBlank() && it != "0" }
            ?: ""

        val bestFollowing = apiProfile?.followingCount?.takeIf { it.isNotBlank() && it != "0" }
            ?: htmlProfile?.followingCount?.takeIf { it.isNotBlank() && it != "0" }
            ?: mirrorProfile?.followingCount?.takeIf { it.isNotBlank() && it != "0" }
            ?: ""

        val bestPostsCount = when {
            apiProfile?.postsCount != null && apiProfile.postsCount > 0 -> apiProfile.postsCount
            htmlProfile?.postsCount != null && htmlProfile.postsCount > 0 -> htmlProfile.postsCount
            mirrorProfile?.postsCount != null && mirrorProfile.postsCount > 0 -> mirrorProfile.postsCount
            else -> mergedReels.size + mergedPosts.size
        }

        val isPrivate = apiProfile?.isPrivate ?: htmlProfile?.isPrivate ?: false
        val isVerified = apiProfile?.isVerified == true || mirrorProfile?.isVerified == true || htmlProfile?.isVerified == true
        val externalUrl = apiProfile?.externalUrl.orEmpty().ifEmpty { htmlProfile?.externalUrl.orEmpty() }

        if (apiProfile != null || mirrorProfile != null || htmlProfile != null) {
            val finalProfile = ProfileData(
                username = cleanUsername,
                fullName = bestFullName,
                bio = bestBio,
                hdAvatarUrl = cleanUrl(bestAvatar),
                isPrivate = isPrivate,
                isVerified = isVerified,
                postsCount = bestPostsCount,
                followersCount = bestFollowers,
                followingCount = bestFollowing,
                externalUrl = externalUrl,
                reels = mergedReels,
                posts = mergedPosts
            )
            Log.d(TAG, "Loaded profile for @$cleanUsername: posts=${mergedPosts.size}, reels=${mergedReels.size}, followers=$bestFollowers")
            return@withContext Result.success(finalProfile)
        }

        Result.failure(Exception("Unable to load profile for @$cleanUsername. Please check your internet connection or verify the username."))
    }

    private fun fetchWebProfileInfo(username: String): ProfileData? {
        val url = "https://www.instagram.com/api/v1/users/web_profile_info/?username=$username"
        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", InstagramHttpClient.USER_AGENT_DESKTOP)
            .header("X-IG-App-ID", IG_APP_ID)
            .header("X-ASBD-ID", ASBD_ID)
            .header("X-IG-WWW-Claim", "0")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Referer", "https://www.instagram.com/$username/")
            .header("Sec-Ch-Ua", "\"Chromium\";v=\"124\", \"Google Chrome\";v=\"124\", \"Not-A.Brand\";v=\"99\"")
            .header("Sec-Ch-Ua-Mobile", "?0")
            .header("Sec-Ch-Ua-Platform", "\"Windows\"")
            .header("Sec-Fetch-Dest", "empty")
            .header("Sec-Fetch-Mode", "cors")
            .header("Sec-Fetch-Site", "same-origin")
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")

        // Attach WebView cookies directly if authenticated
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
        Log.d(TAG, "WebProfileInfo HTTP ${response.code}")
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

        val (reelsList, postsList) = parseUserMedia(user, username, hdAvatar)

        return ProfileData(
            username = username,
            fullName = fullName,
            bio = bio,
            hdAvatarUrl = cleanUrl(hdAvatar),
            isPrivate = isPrivate,
            isVerified = isVerified,
            postsCount = if (postsCountInt > 0) postsCountInt else (reelsList.size + postsList.size),
            followersCount = formatCount(followerCountInt),
            followingCount = formatCount(followingCountInt),
            externalUrl = externalUrl,
            reels = reelsList,
            posts = postsList
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
            val hdAvatar = user.optString("profile_pic_url_hd", user.optString("profile_pic_url", ""))

            val (reelsList, postsList) = parseUserMedia(user, username, hdAvatar)

            ProfileData(
                username = username,
                fullName = user.optString("full_name", username),
                bio = user.optString("biography", ""),
                hdAvatarUrl = user.optString("profile_pic_url_hd", user.optString("profile_pic_url", "")),
                isPrivate = user.optBoolean("is_private", false),
                isVerified = user.optBoolean("is_verified", false),
                postsCount = if (posts > 0) posts else (reelsList.size + postsList.size),
                followersCount = formatCount(followers),
                followingCount = formatCount(following),
                externalUrl = user.optString("external_url", ""),
                reels = reelsList,
                posts = postsList
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

    private fun parseUserMedia(
        user: JSONObject,
        username: String,
        hdAvatar: String
    ): Pair<List<MediaItem>, List<MediaItem>> {
        val reelsMap = LinkedHashMap<String, MediaItem>()
        val postsMap = LinkedHashMap<String, MediaItem>()

        // 1. Clips / Reels
        val clipsEdges = user.optJSONObject("edge_clips_tab")?.optJSONArray("edges")
            ?: user.optJSONObject("edge_felix_video_timeline")?.optJSONArray("edges")
        if (clipsEdges != null) {
            for (i in 0 until clipsEdges.length()) {
                val node = clipsEdges.getJSONObject(i).optJSONObject("node") ?: continue
                val item = parseNodeToMedia(node, username, hdAvatar, forceVideo = true)
                if (item.shortcode.isNotEmpty()) {
                    reelsMap[item.shortcode] = item
                }
            }
        }

        // 2. Timeline Media (12 posts default page on Instagram Web)
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

        return Pair(reelsMap.values.toList(), postsMap.values.toList())
    }

    /**
     * Universal parser that reliably extracts real media cards from any mirror HTML
     */
    private fun extractMediaFromDoc(
        doc: org.jsoup.nodes.Document,
        username: String,
        avatarUrl: String,
        sourcePrefix: String
    ): Pair<List<MediaItem>, List<MediaItem>> {
        val posts = mutableListOf<MediaItem>()
        val reels = mutableListOf<MediaItem>()
        val seenCodes = mutableSetOf<String>()

        val imgTags = doc.select("img")
        for (imgTag in imgTags) {
            val src = imgTag.attr("src").ifEmpty {
                imgTag.attr("data-src").ifEmpty {
                    imgTag.attr("data-original").ifEmpty {
                        imgTag.attr("data-lazy-src")
                    }
                }
            }
            if (src.isBlank()) continue
            val cleanImg = cleanUrl(src)

            // Skip avatars, logos, icons, badges, small banners
            val lower = cleanImg.lowercase()
            if (lower.contains("logo") || lower.contains("icon") || lower.contains("avatar") || cleanImg == avatarUrl) {
                continue
            }
            if (lower.contains("blank.gif") || lower.contains("data:image") || lower.contains("pixel")) {
                continue
            }

            // Find enclosing card / container
            val container = imgTag.parents().firstOrNull { it.tagName() == "div" || it.tagName() == "figure" || it.tagName() == "li" || it.tagName() == "a" } ?: imgTag.parent()

            // Find any link associated with this image
            val allLinks = imgTag.parents().select("a[href]").map { it.attr("href") } +
                    listOfNotNull(imgTag.closest("a")?.attr("href"))
            var code = ""
            for (l in allLinks) {
                val cleanLink = l.trim().trimEnd('/')
                val last = cleanLink.substringAfterLast('/')
                if (last.isNotEmpty() && !last.equals(username, ignoreCase = true) && last.length >= 4) {
                    code = last
                    break
                }
            }

            if (code.isEmpty()) {
                code = "${sourcePrefix}_" + Math.abs(cleanImg.hashCode()).toString(36)
            }

            if (!seenCodes.add(code)) continue

            val isVideo = container?.selectFirst(".video, .icon_video, .video-icon, .play, video, [data-type='video']") != null ||
                    allLinks.any { it.contains("/reel/") || it.contains("/tv/") }

            val caption = container?.selectFirst(".caption, .desc, .title, p")?.text()?.trim() ?: ""

            val item = MediaItem(
                id = code,
                shortcode = code,
                type = if (isVideo) MediaType.REEL else MediaType.POST_IMAGE,
                url = if (code.startsWith(sourcePrefix)) "https://www.instagram.com/$username/" else "https://www.instagram.com/p/$code/",
                downloadUrl = cleanImg,
                thumbnailUrl = cleanImg,
                title = (if (isVideo) "Reel" else "Post") + " by @$username",
                caption = caption,
                authorUsername = username,
                authorAvatarUrl = avatarUrl
            )

            if (isVideo) reels.add(item) else posts.add(item)
        }

        return Pair(reels, posts)
    }

    /**
     * Tier 2 Public Mirror layer (Greatfon / Dumpor / Picuki / Imginn / Pixwox / StoriesIG / AnonyIG)
     */
    private fun fetchProfileFromMirror(username: String): ProfileData? {
        Log.d(TAG, "Trying mirror 1: Greatfon")
        val greatfon = runCatching { fetchFromGreatfon(username) }
        greatfon.getOrNull()?.let {
            if (it.posts.isNotEmpty() || it.reels.isNotEmpty()) return it
        }

        Log.d(TAG, "Trying mirror 2: Dumpor")
        val dumpor = runCatching { fetchFromDumpor(username) }
        dumpor.getOrNull()?.let {
            if (it.posts.isNotEmpty() || it.reels.isNotEmpty()) return it
        }

        Log.d(TAG, "Trying mirror 3: Picuki")
        val picuki = runCatching { fetchFromPicuki(username) }
        picuki.getOrNull()?.let {
            if (it.posts.isNotEmpty() || it.reels.isNotEmpty()) return it
        }

        Log.d(TAG, "Trying mirror 4: Imginn")
        val imginn = runCatching { fetchFromImginn(username) }
        imginn.getOrNull()?.let {
            if (it.posts.isNotEmpty() || it.reels.isNotEmpty()) return it
        }

        Log.d(TAG, "Trying mirror 5: Pixwox")
        val pixwox = runCatching { fetchFromPixwox(username) }
        pixwox.getOrNull()?.let {
            if (it.posts.isNotEmpty() || it.reels.isNotEmpty()) return it
        }

        Log.d(TAG, "Trying mirror 6: StoriesIG")
        val stories = runCatching { fetchFromStoriesIG(username) }
        stories.getOrNull()?.let {
            if (it.posts.isNotEmpty() || it.reels.isNotEmpty()) return it
        }

        Log.d(TAG, "Trying mirror 7: AnonyIG")
        val anony = runCatching { fetchFromAnonyIG(username) }
        anony.getOrNull()?.let {
            if (it.posts.isNotEmpty() || it.reels.isNotEmpty()) return it
        }

        return greatfon.getOrNull() ?: dumpor.getOrNull() ?: picuki.getOrNull() ?: imginn.getOrNull() ?: pixwox.getOrNull() ?: stories.getOrNull() ?: anony.getOrNull()
    }

    private fun fetchFromGreatfon(username: String): ProfileData? {
        val url = "https://greatfon.io/v/$username"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", InstagramHttpClient.USER_AGENT_DESKTOP)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Referer", "https://greatfon.io/")
            .build()
        val response = InstagramHttpClient.client.newCall(request).execute()
        Log.d(TAG, "Greatfon response: ${response.code}")
        if (!response.isSuccessful) return null
        val html = response.body?.string() ?: return null
        val doc = Jsoup.parse(html)

        val avatar = doc.selectFirst(".user-avatar img, .avatar img, img.avatar")?.attr("src") ?: ""
        val fullName = doc.selectFirst(".user__title h1, h1")?.text()?.trim() ?: username
        val bio = doc.selectFirst(".user__info-desc, .bio")?.text()?.trim() ?: ""

        val (reels, posts) = extractMediaFromDoc(doc, username, cleanUrl(avatar), "gf")
        Log.d(TAG, "Greatfon universal extracted: posts=${posts.size}, reels=${reels.size}")
        if (posts.isEmpty() && reels.isEmpty() && bio.isEmpty()) return null

        return ProfileData(
            username = username,
            fullName = fullName.ifEmpty { username },
            bio = bio,
            hdAvatarUrl = cleanUrl(avatar),
            isPrivate = false,
            isVerified = false,
            postsCount = reels.size + posts.size,
            followersCount = "",
            followingCount = "",
            externalUrl = "",
            reels = reels,
            posts = posts
        )
    }

    private fun fetchFromDumpor(username: String): ProfileData? {
        val url = "https://dumpor.io/v/$username"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", InstagramHttpClient.USER_AGENT_DESKTOP)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Referer", "https://dumpor.io/")
            .build()
        val response = InstagramHttpClient.client.newCall(request).execute()
        Log.d(TAG, "Dumpor response: ${response.code}")
        if (!response.isSuccessful) return null
        val html = response.body?.string() ?: return null
        val doc = Jsoup.parse(html)

        val avatar = doc.selectFirst(".user-avatar img, .avatar img")?.attr("src") ?: ""
        val fullName = doc.selectFirst(".user__title h1, h1")?.text()?.trim() ?: username
        val bio = doc.selectFirst(".user__info-desc")?.text()?.trim() ?: ""

        val (reels, posts) = extractMediaFromDoc(doc, username, cleanUrl(avatar), "dumpor")
        Log.d(TAG, "Dumpor universal extracted: posts=${posts.size}, reels=${reels.size}")
        if (posts.isEmpty() && reels.isEmpty() && bio.isEmpty()) return null

        return ProfileData(
            username = username,
            fullName = fullName.ifEmpty { username },
            bio = bio,
            hdAvatarUrl = cleanUrl(avatar),
            isPrivate = false,
            isVerified = false,
            postsCount = reels.size + posts.size,
            followersCount = "",
            followingCount = "",
            externalUrl = "",
            reels = reels,
            posts = posts
        )
    }

    private fun fetchFromPicuki(username: String): ProfileData? {
        val url = "https://www.picuki.com/profile/$username"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", InstagramHttpClient.USER_AGENT_DESKTOP)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Referer", "https://www.picuki.com/")
            .build()
        val response = InstagramHttpClient.client.newCall(request).execute()
        Log.d(TAG, "Picuki response: ${response.code}")
        if (!response.isSuccessful) return null
        val html = response.body?.string() ?: return null
        val doc = Jsoup.parse(html)

        val avatar = doc.selectFirst(".profile-avatar img")?.attr("src")
            ?: doc.selectFirst(".profile-avatar")?.attr("src") ?: ""
        val fullName = doc.selectFirst(".profile-name-bottom")?.text()?.trim() ?: username
        val bio = doc.selectFirst(".profile-description")?.text()?.trim() ?: ""
        val isVerified = doc.selectFirst(".verified-icon") != null || doc.selectFirst("i.fa-check-circle") != null

        val (reels, posts) = extractMediaFromDoc(doc, username, cleanUrl(avatar), "picuki")

        val stats = doc.select(".profile-statistics span")
        var followers = ""
        var following = ""
        var postsCount = 0
        for (span in stats) {
            val text = span.text()
            if (text.contains("Followers", ignoreCase = true)) {
                followers = text.replace(Regex("[^0-9.KMBkmb]"), "")
            } else if (text.contains("Following", ignoreCase = true)) {
                following = text.replace(Regex("[^0-9.KMBkmb]"), "")
            } else if (text.contains("Posts", ignoreCase = true)) {
                postsCount = text.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
            }
        }

        return ProfileData(
            username = username,
            fullName = fullName.ifEmpty { username },
            bio = bio,
            hdAvatarUrl = cleanUrl(avatar),
            isPrivate = false,
            isVerified = isVerified,
            postsCount = if (postsCount > 0) postsCount else (reels.size + posts.size),
            followersCount = followers,
            followingCount = following,
            externalUrl = "",
            reels = reels,
            posts = posts
        )
    }

    private fun fetchFromImginn(username: String): ProfileData? {
        val url = "https://imginn.com/$username/"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", InstagramHttpClient.USER_AGENT_DESKTOP)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Referer", "https://imginn.com/")
            .build()
        val response = InstagramHttpClient.client.newCall(request).execute()
        Log.d(TAG, "Imginn response: ${response.code}")
        if (!response.isSuccessful) return null
        val html = response.body?.string() ?: return null
        val doc = Jsoup.parse(html)

        val avatar = doc.selectFirst(".avatar img")?.attr("src") ?: ""
        val fullName = doc.selectFirst(".username, h1, .name")?.text()?.trim() ?: username
        val bio = doc.selectFirst(".desc, .bio")?.text()?.trim() ?: ""

        val (reels, posts) = extractMediaFromDoc(doc, username, cleanUrl(avatar), "imginn")

        val stats = doc.select(".info span, .stats span, .count span, .num")
        var followers = ""
        var following = ""
        var postsCount = 0
        for (span in stats) {
            val text = span.text()
            if (text.contains("Follower", ignoreCase = true)) {
                followers = text.replace(Regex("[^0-9.KMBkmb]"), "")
            } else if (text.contains("Following", ignoreCase = true)) {
                following = text.replace(Regex("[^0-9.KMBkmb]"), "")
            } else if (text.contains("Post", ignoreCase = true)) {
                postsCount = text.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
            }
        }

        if (posts.isEmpty() && reels.isEmpty() && bio.isEmpty()) return null

        return ProfileData(
            username = username,
            fullName = fullName.ifEmpty { username },
            bio = bio,
            hdAvatarUrl = cleanUrl(avatar),
            isPrivate = false,
            isVerified = false,
            postsCount = if (postsCount > 0) postsCount else (reels.size + posts.size),
            followersCount = followers,
            followingCount = following,
            externalUrl = "",
            reels = reels,
            posts = posts
        )
    }

    private fun fetchFromPixwox(username: String): ProfileData? {
        val url = "https://www.pixwox.com/profile/$username/"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", InstagramHttpClient.USER_AGENT_DESKTOP)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Referer", "https://www.pixwox.com/")
            .build()
        val response = InstagramHttpClient.client.newCall(request).execute()
        Log.d(TAG, "Pixwox response: ${response.code}")
        if (!response.isSuccessful) return null
        val html = response.body?.string() ?: return null
        val doc = Jsoup.parse(html)

        val avatar = doc.selectFirst(".head .avatar img")?.attr("src")
            ?: doc.selectFirst(".avatar img")?.attr("src") ?: ""
        val fullName = doc.selectFirst(".fullname")?.text()?.trim()
            ?: doc.selectFirst("h1")?.text()?.trim() ?: username
        val bio = doc.selectFirst(".desc")?.text()?.trim() ?: ""

        val (reels, posts) = extractMediaFromDoc(doc, username, cleanUrl(avatar), "pixwox")

        val stats = doc.select(".sum_num")
        var postsCount = 0
        var followers = ""
        var following = ""
        if (stats.size >= 3) {
            postsCount = stats[0].text().replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
            followers = stats[1].text().trim()
            following = stats[2].text().trim()
        }

        if (posts.isEmpty() && reels.isEmpty() && bio.isEmpty() && followers.isEmpty()) return null

        return ProfileData(
            username = username,
            fullName = fullName.ifEmpty { username },
            bio = bio,
            hdAvatarUrl = cleanUrl(avatar),
            isPrivate = false,
            isVerified = doc.selectFirst(".verified, i.fa-check") != null,
            postsCount = if (postsCount > 0) postsCount else (reels.size + posts.size),
            followersCount = followers,
            followingCount = following,
            externalUrl = "",
            reels = reels,
            posts = posts
        )
    }

    private fun fetchFromStoriesIG(username: String): ProfileData? {
        val url = "https://storiesig.info/en/$username/"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", InstagramHttpClient.USER_AGENT_DESKTOP)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Referer", "https://storiesig.info/")
            .build()
        val response = InstagramHttpClient.client.newCall(request).execute()
        Log.d(TAG, "StoriesIG response: ${response.code}")
        if (!response.isSuccessful) return null
        val html = response.body?.string() ?: return null
        val doc = Jsoup.parse(html)

        val avatar = doc.selectFirst(".profile__avatar img, .avatar img, img.avatar")?.attr("src") ?: ""
        val fullName = doc.selectFirst(".profile__name, h1")?.text()?.trim() ?: username
        val bio = doc.selectFirst(".profile__biography, .bio")?.text()?.trim() ?: ""

        val (reels, posts) = extractMediaFromDoc(doc, username, cleanUrl(avatar), "stories")
        if (posts.isEmpty() && reels.isEmpty() && bio.isEmpty()) return null

        return ProfileData(
            username = username,
            fullName = fullName.ifEmpty { username },
            bio = bio,
            hdAvatarUrl = cleanUrl(avatar),
            isPrivate = false,
            isVerified = false,
            postsCount = reels.size + posts.size,
            followersCount = "",
            followingCount = "",
            externalUrl = "",
            reels = reels,
            posts = posts
        )
    }

    private fun fetchFromAnonyIG(username: String): ProfileData? {
        val url = "https://anonyig.com/en/profile/$username/"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", InstagramHttpClient.USER_AGENT_DESKTOP)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Referer", "https://anonyig.com/")
            .build()
        val response = InstagramHttpClient.client.newCall(request).execute()
        Log.d(TAG, "AnonyIG response: ${response.code}")
        if (!response.isSuccessful) return null
        val html = response.body?.string() ?: return null
        val doc = Jsoup.parse(html)

        val avatar = doc.selectFirst(".profile__avatar img, img.avatar")?.attr("src") ?: ""
        val fullName = doc.selectFirst(".profile__name, h1")?.text()?.trim() ?: username
        val bio = doc.selectFirst(".profile__biography")?.text()?.trim() ?: ""

        val (reels, posts) = extractMediaFromDoc(doc, username, cleanUrl(avatar), "anony")
        if (posts.isEmpty() && reels.isEmpty() && bio.isEmpty()) return null

        return ProfileData(
            username = username,
            fullName = fullName.ifEmpty { username },
            bio = bio,
            hdAvatarUrl = cleanUrl(avatar),
            isPrivate = false,
            isVerified = false,
            postsCount = reels.size + posts.size,
            followersCount = "",
            followingCount = "",
            externalUrl = "",
            reels = reels,
            posts = posts
        )
    }

    /**
     * Deep HTML parser that extracts stats and embedded media from HTML & Meta-tags
     */
    private fun fetchProfileFromHtmlWithScraping(username: String): ProfileData? {
        val pageUrl = "https://www.instagram.com/$username/"
        val requestBuilder = Request.Builder()
            .url(pageUrl)
            .header("User-Agent", InstagramHttpClient.USER_AGENT_DESKTOP)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "none")
            .header("Sec-Fetch-User", "?1")
            .header("Upgrade-Insecure-Requests", "1")

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
        Log.d(TAG, "Instagram HTML response: ${response.code}")
        if (!response.isSuccessful) return null
        val html = response.body?.string() ?: return null

        val doc = Jsoup.parse(html)
        val ogTitle = doc.selectFirst("meta[property=og:title]")?.attr("content") ?: username
        val ogDesc = doc.selectFirst("meta[property=og:description]")?.attr("content") ?: ""
        val ogImage = doc.selectFirst("meta[property=og:image]")?.attr("content") ?: ""

        var followersFormatted = ""
        var followingFormatted = ""
        var postsCount = 0

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

        // Clean custom bio text without metadata repetition
        val cleanBio = if (normalizedDesc.contains("photos and videos from")) {
            val afterBrackets = normalizedDesc.substringAfter("):", "").trim()
            if (afterBrackets.isNotEmpty()) afterBrackets else ""
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

        // Scan script tags for shortcodes & media items atomically within JSON window
        val scripts = doc.select("script")
        val shortcodeRegex = Pattern.compile("\"(?:shortcode|code)\"\\s*:\\s*\"([A-Za-z0-9_-]{8,15})\"")
        val displayUrlRegex = Pattern.compile("\"(?:display_url|thumbnail_src)\"\\s*:\\s*\"(https:[^\"\\\\]+)\"")
        val videoUrlRegex = Pattern.compile("\"video_url\"\\s*:\\s*\"(https:[^\"\\\\]+)\"")

        for (script in scripts) {
            val content = script.html()
            if (!content.contains("\"shortcode\":") && !content.contains("\"code\":")) continue

            val matcher = shortcodeRegex.matcher(content)
            while (matcher.find()) {
                val code = matcher.group(1) ?: continue
                if (reelsMap.containsKey(code) || postsMap.containsKey(code)) continue

                // Find local window of 2500 chars around this match
                val start = maxOf(0, matcher.start() - 1500)
                val end = minOf(content.length, matcher.end() + 1500)
                val window = content.substring(start, end)

                val isVid = window.contains("\"is_video\":true") || window.contains("\"is_video\": true")
                val dispMatch = displayUrlRegex.matcher(window)
                val rawDisplayUrl = if (dispMatch.find()) dispMatch.group(1) else ""
                val vidMatch = videoUrlRegex.matcher(window)
                val rawVideoUrl = if (vidMatch.find()) vidMatch.group(1) else ""

                val effectiveImg = cleanUrl(rawDisplayUrl.replace("\\u0026", "&"))
                val effectiveVid = cleanUrl(rawVideoUrl.replace("\\u0026", "&"))

                if (effectiveImg.isNotEmpty() || effectiveVid.isNotEmpty()) {
                    val item = MediaItem(
                        id = code,
                        shortcode = code,
                        type = if (isVid) MediaType.REEL else MediaType.POST_IMAGE,
                        url = "https://www.instagram.com/p/$code/",
                        downloadUrl = if (isVid && effectiveVid.isNotEmpty()) effectiveVid else effectiveImg,
                        thumbnailUrl = effectiveImg.ifEmpty { effectiveVid },
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
            count >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }

    private fun cleanUrl(url: String): String {
        if (url.isBlank()) return ""
        var cleaned = url.replace("&amp;", "&").replace("\\u0026", "&")
        if (cleaned.startsWith("//")) {
            cleaned = "https:$cleaned"
        }
        return cleaned
    }
}
