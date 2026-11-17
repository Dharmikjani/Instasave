package com.instasaver.feature.downloader.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instasaver.core.database.DatabaseRepository
import com.instasaver.core.extractor.ExtractorRepository
import com.instasaver.core.model.AudioTrack
import com.instasaver.core.model.DownloadProgress
import com.instasaver.core.model.MediaItem
import com.instasaver.core.model.MediaType
import com.instasaver.feature.downloader.work.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DownloaderUiState {
    object Idle : DownloaderUiState
    object Loading : DownloaderUiState
    data class Success(val mediaItem: MediaItem) : DownloaderUiState
    data class Error(val message: String) : DownloaderUiState
}

sealed interface DownloaderUiIntent {
    data class FetchUrl(val url: String) : DownloaderUiIntent
    data class DownloadMedia(val item: MediaItem, val customUrl: String? = null) : DownloaderUiIntent
    data class DownloadAudio(val item: MediaItem) : DownloaderUiIntent
    data class PinCreator(val username: String) : DownloaderUiIntent
    object Clear : DownloaderUiIntent
}

@HiltViewModel
class DownloaderViewModel @Inject constructor(
    private val extractorRepository: ExtractorRepository,
    private val downloadRepository: DownloadRepository,
    private val databaseRepository: DatabaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DownloaderUiState>(DownloaderUiState.Idle)
    val uiState: StateFlow<DownloaderUiState> = _uiState.asStateFlow()

    val downloadStates: StateFlow<Map<String, DownloadProgress>> = downloadRepository.downloadStates

    fun onIntent(intent: DownloaderUiIntent) {
        when (intent) {
            is DownloaderUiIntent.FetchUrl -> fetchUrl(intent.url)
            is DownloaderUiIntent.DownloadMedia -> downloadMedia(intent.item, intent.customUrl)
            is DownloaderUiIntent.DownloadAudio -> downloadAudio(intent.item)
            is DownloaderUiIntent.PinCreator -> pinCreator(intent.username)
            is DownloaderUiIntent.Clear -> _uiState.value = DownloaderUiState.Idle
        }
    }

    private fun fetchUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            _uiState.value = DownloaderUiState.Error("Please enter a valid Instagram URL.")
            return
        }

        viewModelScope.launch {
            _uiState.value = DownloaderUiState.Loading
            val result = extractorRepository.extractMedia(trimmed)
            result.onSuccess { media ->
                _uiState.value = DownloaderUiState.Success(media)
            }.onFailure { ex ->
                _uiState.value = DownloaderUiState.Error(ex.message ?: "Failed to extract media.")
            }
        }
    }

    private fun downloadMedia(
        item: MediaItem,
        customUrl: String? = null,
        onSuccess: (Uri) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        downloadRepository.enqueueDownload(item, customUrl, onSuccess, onError)
    }

    private fun downloadAudio(item: MediaItem) {
        val audioTrack = item.audioTrack
        if (audioTrack != null && audioTrack.audioUrl.isNotBlank()) {
            val audioItem = item.copy(
                type = MediaType.AUDIO,
                downloadUrl = audioTrack.audioUrl,
                title = "Audio - ${audioTrack.title}"
            )
            downloadRepository.enqueueDownload(audioItem)
        } else {
            val audioItem = item.copy(
                type = MediaType.AUDIO,
                title = "Audio - ${item.authorUsername}"
            )
            downloadRepository.enqueueDownload(audioItem)
        }
    }

    private fun pinCreator(username: String) {
        viewModelScope.launch {
            if (username.isNotBlank()) {
                databaseRepository.pinCreator(username)
            }
        }
    }
}
