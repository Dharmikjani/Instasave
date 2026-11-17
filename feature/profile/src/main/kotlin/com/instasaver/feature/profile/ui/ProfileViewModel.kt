package com.instasaver.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instasaver.core.database.DatabaseRepository
import com.instasaver.core.extractor.ExtractorRepository
import com.instasaver.core.extractor.ProfileRepository
import com.instasaver.core.model.MediaType
import com.instasaver.core.model.MediaItem
import com.instasaver.core.model.ProfileData
import com.instasaver.core.storage.MediaStorageService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    data class DownloadAudio(val item: MediaItem) : ProfileUiIntent
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val extractorRepository: ExtractorRepository,
    private val databaseRepository: DatabaseRepository,
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
            is ProfileUiIntent.DownloadAudio -> downloadAudio(intent.item)
        }
    }

    private fun sanitizeInput(input: String): String {
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

    private fun searchProfile(username: String) {
        val clean = sanitizeInput(username)
        if (clean.isBlank()) {
            _uiState.value = ProfileUiState.Error("Please enter a valid Instagram username.")
            return
        }

        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val isPinned = databaseRepository.isPinned(clean)
            val result = profileRepository.getProfile(clean)
            result.onSuccess { data ->
                _uiState.value = ProfileUiState.Success(data, isPinned)
            }.onFailure { ex ->
                _uiState.value = ProfileUiState.Error(ex.message ?: "Failed to load profile for @$clean.")
            }
        }
    }

    private fun togglePin(username: String) {
        val clean = sanitizeInput(username)
        viewModelScope.launch {
            val currentlyPinned = databaseRepository.isPinned(clean)
            if (currentlyPinned) {
                databaseRepository.unpinCreator(clean)
            } else {
                databaseRepository.pinCreator(clean)
            }
            val currentState = _uiState.value
            if (currentState is ProfileUiState.Success && currentState.profile.username.equals(clean, ignoreCase = true)) {
                _uiState.value = currentState.copy(isPinned = !currentlyPinned)
            }
        }
    }

    private fun resolveMediaUrl(item: MediaItem): suspend () -> MediaItem = {
        if (item.type == MediaType.REEL && (item.downloadUrl.endsWith(".jpg") || item.downloadUrl.endsWith(".jpeg") || item.downloadUrl == item.thumbnailUrl)) {
            val fresh = extractorRepository.extractMedia(item.url)
            if (fresh.isSuccess) fresh.getOrThrow() else item
        } else {
            item
        }
    }

    private fun downloadItem(item: MediaItem) {
        viewModelScope.launch {
            val targetItem = resolveMediaUrl(item)()
            val result = storageService.saveMediaToGallery(
                downloadUrl = targetItem.downloadUrl,
                authorUsername = targetItem.authorUsername,
                shortcode = targetItem.shortcode,
                mediaType = targetItem.type
            )
            result.onSuccess { uri ->
                databaseRepository.recordDownload(
                    shortcode = targetItem.shortcode,
                    fileName = targetItem.title.ifBlank { "Media by @${targetItem.authorUsername}" },
                    savedUri = uri.toString()
                )
            }
        }
    }

    private fun downloadAudio(item: MediaItem) {
        viewModelScope.launch {
            val targetItem = resolveMediaUrl(item)()
            val audioUrl = targetItem.audioTrack?.audioUrl ?: targetItem.downloadUrl
            val result = storageService.saveMediaToGallery(
                downloadUrl = audioUrl,
                authorUsername = targetItem.authorUsername,
                shortcode = targetItem.shortcode,
                mediaType = MediaType.AUDIO
            )
            result.onSuccess { uri ->
                databaseRepository.recordDownload(
                    shortcode = targetItem.shortcode,
                    fileName = "Audio - @${targetItem.authorUsername}",
                    savedUri = uri.toString()
                )
            }
        }
    }

    private fun downloadBatch(items: List<MediaItem>) {
        viewModelScope.launch {
            for (item in items) {
                val targetItem = resolveMediaUrl(item)()
                val result = storageService.saveMediaToGallery(
                    downloadUrl = targetItem.downloadUrl,
                    authorUsername = targetItem.authorUsername,
                    shortcode = targetItem.shortcode,
                    mediaType = targetItem.type
                )
                result.onSuccess { uri ->
                    databaseRepository.recordDownload(
                        shortcode = targetItem.shortcode,
                        fileName = targetItem.title.ifBlank { "Media by @${targetItem.authorUsername}" },
                        savedUri = uri.toString()
                    )
                }
                kotlinx.coroutines.delay(350)
            }
        }
    }
}
