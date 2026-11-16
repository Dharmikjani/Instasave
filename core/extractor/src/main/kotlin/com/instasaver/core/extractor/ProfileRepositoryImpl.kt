package com.instasaver.core.extractor

import com.instasaver.core.model.ProfileData
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val okHttpClient: OkHttpClient
) : ProfileRepository {

    override suspend fun getProfile(username: String): Result<ProfileData> {
        return ProfileScraper.getProfile(username)
    }
}
