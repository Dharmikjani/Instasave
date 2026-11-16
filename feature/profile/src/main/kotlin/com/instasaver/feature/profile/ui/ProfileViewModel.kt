package com.instasaver.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instasaver.core.database.dao.WatchlistDao
import com.instasaver.core.database.entity.CreatorEntity
import com.instasaver.core.extractor.ProfileRepository
import com.instasaver.core.model.MediaItem
import com.instasaver.core.model.ProfileData
import com.instasaver.core.storage.MediaStorageService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface ProfileUiState {
    object Idle : ProfileUiState
    object Loading : ProfileUiState
    data class Success(val profile: ProfileData, val isPinned: Boolean) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

sealed interface ProfileUiIntent {
    data class Search(val username: String) : ProfileUiIntent
    data class TogglePin(val username: String) : ProfileUiIntent
    data class DownloadItem(val item: MediaItem) : ProfileUiIntent
    data class DownloadBatch(val items: List<MediaItem>) : ProfileUiIntent
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val watchlistDao: WatchlistDao,
    private val storageService: MediaStorageService
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun onIntent(intent: ProfileUiIntent) {
        when (intent) {
            is ProfileUiIntent.Search -> searchProfile(intent.username)
            is ProfileUiIntent.TogglePin -> togglePin(intent.username)
            is ProfileUiIntent.DownloadItem -> downloadItem(intent.item)
            is ProfileUiIntent.DownloadBatch -> downloadBatch(intent.items)
        }
    }

    private fun searchProfile(username: String) {
        val clean = username.trim().removePrefix("@")
        if (clean.isBlank()) {
            _uiState.value = ProfileUiState.Error("Please enter a valid Instagram username.")
            return
        }

        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val isPinned = withContext(Dispatchers.IO) { watchlistDao.isPinned(clean) }
            val result = profileRepository.getProfile(clean)
            result.onSuccess { data ->
                _uiState.value = ProfileUiState.Success(data, isPinned)
            }.onFailure { ex ->
                _uiState.value = ProfileUiState.Error(ex.message ?: "Failed to load profile for @$clean.")
            }
        }
    }

    private fun togglePin(username: String) {
        val clean = username.trim().removePrefix("@")
        viewModelScope.launch {
            val currentlyPinned = withContext(Dispatchers.IO) { watchlistDao.isPinned(clean) }
            withContext(Dispatchers.IO) {
                if (currentlyPinned) {
                    watchlistDao.delete(clean)
                } else {
                    watchlistDao.insert(CreatorEntity(username = clean))
                }
            }
            val currentState = _uiState.value
            if (currentState is ProfileUiState.Success && currentState.profile.username.equals(clean, ignoreCase = true)) {
                _uiState.value = currentState.copy(isPinned = !currentlyPinned)
            }
        }
    }

    private fun downloadItem(item: MediaItem) {
        viewModelScope.launch {
            storageService.saveMediaToGallery(
                downloadUrl = item.downloadUrl,
                authorUsername = item.authorUsername,
                shortcode = item.shortcode,
                mediaType = item.type
            )
        }
    }

    private fun downloadBatch(items: List<MediaItem>) {
        viewModelScope.launch {
            for (item in items) {
                storageService.saveMediaToGallery(
                    downloadUrl = item.downloadUrl,
                    authorUsername = item.authorUsername,
                    shortcode = item.shortcode,
                    mediaType = item.type
                )
            }
        }
    }
}
