package com.instasaver.core.model

import android.net.Uri
import kotlinx.serialization.Serializable

enum class MediaType {
    REEL,
    POST_IMAGE,
    POST_VIDEO,
    CAROUSEL,
    STORY,
    HIGHLIGHT,
    PROFILE_DP,
    AUDIO
}

@Serializable
data class MediaItem(
    val id: String,
    val shortcode: String,
    val type: MediaType,
    val url: String,
    val downloadUrl: String,
    val thumbnailUrl: String,
    val title: String = "",
    val caption: String = "",
    val hashtags: List<String> = emptyList(),
    val authorUsername: String = "",
    val authorFullName: String = "",
    val authorAvatarUrl: String = "",
    val durationSeconds: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val carouselItems: List<CarouselSlide> = emptyList(),
    val audioTrack: AudioTrack? = null
)

@Serializable
data class CarouselSlide(
    val index: Int,
    val type: MediaType,
    val downloadUrl: String,
    val thumbnailUrl: String
)

@Serializable
data class AudioTrack(
    val title: String,
    val artist: String,
    val audioUrl: String,
    val coverUrl: String
)

@Serializable
data class ProfileData(
    val username: String,
    val fullName: String,
    val bio: String,
    val hdAvatarUrl: String,
    val isPrivate: Boolean,
    val isVerified: Boolean = false,
    val postsCount: Int = 0,
    val followersCount: String = "",
    val followingCount: String = "",
    val externalUrl: String = "",
    val stories: List<MediaItem> = emptyList(),
    val highlights: List<MediaItem> = emptyList(),
    val reels: List<MediaItem> = emptyList(),
    val posts: List<MediaItem> = emptyList()
)

data class DownloadProgress(
    val mediaId: String,
    val fileName: String,
    val progress: Float, // 0.0 to 1.0
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val isCompleted: Boolean = false,
    val error: String? = null
)

data class LocalSavedItem(
    val id: Long,
    val title: String,
    val uri: Uri,
    val isVideo: Boolean,
    val creatorName: String,
    val sizeBytes: Long,
    val dateAdded: Long
)
