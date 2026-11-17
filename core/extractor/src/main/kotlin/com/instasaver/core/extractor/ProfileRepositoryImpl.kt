package com.instasaver.core.extractor

import com.instasaver.core.database.DatabaseRepository
import com.instasaver.core.model.ProfileData
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val databaseRepository: DatabaseRepository
) : ProfileRepository {

    override suspend fun getProfile(username: String): Result<ProfileData> {
        val cleanUser = username.trim().removePrefix("@").lowercase()
        if (cleanUser.isNotBlank()) {
            // Check Tier 0 Cache (6h TTL)
            val cached = databaseRepository.getCachedProfile(cleanUser)
            if (cached != null) {
                return Result.success(cached)
            }
        }

        val result = ProfileScraper.getProfile(cleanUser)
        result.onSuccess { profile ->
            if (profile.username.isNotBlank()) {
                databaseRepository.saveProfileCache(profile.username, profile)
            }
        }
        return result
    }
}
