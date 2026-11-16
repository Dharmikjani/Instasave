package com.instasaver.feature.watchlist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instasaver.core.database.dao.WatchlistDao
import com.instasaver.core.database.entity.CreatorEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface WatchlistUiIntent {
    data class RemoveCreator(val username: String) : WatchlistUiIntent
    data class ToggleCreator(val username: String) : WatchlistUiIntent
}

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val watchlistDao: WatchlistDao
) : ViewModel() {

    val creators: StateFlow<List<String>> = watchlistDao.observePinnedUsernames()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onIntent(intent: WatchlistUiIntent) {
        when (intent) {
            is WatchlistUiIntent.RemoveCreator -> removeCreator(intent.username)
            is WatchlistUiIntent.ToggleCreator -> toggleCreator(intent.username)
        }
    }

    private fun removeCreator(username: String) {
        val clean = username.trim().removePrefix("@")
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                watchlistDao.delete(clean)
            }
        }
    }

    private fun toggleCreator(username: String) {
        val clean = username.trim().removePrefix("@")
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (watchlistDao.isPinned(clean)) {
                    watchlistDao.delete(clean)
                } else {
                    watchlistDao.insert(CreatorEntity(username = clean))
                }
            }
        }
    }
}
