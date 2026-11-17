package com.instasaver.core.extractor

import com.instasaver.core.model.MediaItem
import com.instasaver.core.model.ProfileData

interface ProfileRepository {
    suspend fun getProfile(username: String): Result<ProfileData>
    suspend fun loadMoreMedia(username: String, page: Int): Result<Pair<List<MediaItem>, List<MediaItem>>>
}
