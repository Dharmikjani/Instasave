package com.instasaver.app.core.watchlist

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

object WatchlistManager {

    private const val PREFS_NAME = "instasaver_watchlist_prefs"
    private const val KEY_CREATORS = "pinned_creators"

    private lateinit var prefs: SharedPreferences
    private val _creators = MutableStateFlow<List<String>>(emptyList())
    val creators: StateFlow<List<String>> = _creators.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadWatchlist()
    }

    private fun loadWatchlist() {
        val raw = prefs.getString(KEY_CREATORS, "[]") ?: "[]"
        val jsonArray = JSONArray(raw)
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        _creators.value = list
    }

    fun toggleCreator(username: String): Boolean {
        val clean = username.trim().removePrefix("@")
        val current = _creators.value.toMutableList()
        val isAdded: Boolean
        if (current.contains(clean)) {
            current.remove(clean)
            isAdded = false
        } else {
            current.add(0, clean)
            isAdded = true
        }

        val jsonArray = JSONArray(current)
        prefs.edit().putString(KEY_CREATORS, jsonArray.toString()).apply()
        _creators.value = current
        return isAdded
    }

    fun isPinned(username: String): Boolean {
        return _creators.value.contains(username.trim().removePrefix("@"))
    }
}

