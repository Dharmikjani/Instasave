package com.instasaver.core.extractor

import com.instasaver.core.model.ProfileData

interface ProfileRepository {
    suspend fun getProfile(username: String): Result<ProfileData>
}
