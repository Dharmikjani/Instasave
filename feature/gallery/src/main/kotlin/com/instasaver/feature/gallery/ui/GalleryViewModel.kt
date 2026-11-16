package com.instasaver.feature.gallery.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instasaver.core.model.LocalSavedItem
import com.instasaver.feature.gallery.data.GalleryScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface GalleryUiState {
    object Loading : GalleryUiState
    data class Success(
        val allItems: List<LocalSavedItem>,
        val filteredItems: List<LocalSavedItem>,
        val creators: List<String>,
        val selectedCreator: String?
    ) : GalleryUiState
    object Empty : GalleryUiState
}

sealed interface GalleryUiIntent {
    object LoadMedia : GalleryUiIntent
    data class FilterCreator(val creator: String?) : GalleryUiIntent
}

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val galleryScanner: GalleryScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading)
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private var cachedItems: List<LocalSavedItem> = emptyList()
    private var selectedCreator: String? = null

    init {
        loadMedia()
    }

    fun onIntent(intent: GalleryUiIntent) {
        when (intent) {
            is GalleryUiIntent.LoadMedia -> loadMedia()
            is GalleryUiIntent.FilterCreator -> filterByCreator(intent.creator)
        }
    }

    fun loadMedia() {
        viewModelScope.launch {
            _uiState.value = GalleryUiState.Loading
            val items = galleryScanner.loadSavedMedia()
            cachedItems = items
            if (items.isEmpty()) {
                _uiState.value = GalleryUiState.Empty
            } else {
                val creators = items.map { it.creatorName }.distinct().sorted()
                val filtered = if (selectedCreator != null) {
                    items.filter { it.creatorName.equals(selectedCreator, ignoreCase = true) }
                } else items

                _uiState.value = GalleryUiState.Success(
                    allItems = items,
                    filteredItems = filtered,
                    creators = creators,
                    selectedCreator = selectedCreator
                )
            }
        }
    }

    private fun filterByCreator(creator: String?) {
        selectedCreator = creator
        if (cachedItems.isEmpty()) {
            _uiState.value = GalleryUiState.Empty
            return
        }
        val creators = cachedItems.map { it.creatorName }.distinct().sorted()
        val filtered = if (creator != null) {
            cachedItems.filter { it.creatorName.equals(creator, ignoreCase = true) }
        } else cachedItems

        _uiState.value = GalleryUiState.Success(
            allItems = cachedItems,
            filteredItems = filtered,
            creators = creators,
            selectedCreator = creator
        )
    }
}
