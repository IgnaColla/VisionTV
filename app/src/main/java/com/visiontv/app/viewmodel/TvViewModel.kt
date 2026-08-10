package com.visiontv.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.visiontv.app.data.model.Channel
import com.visiontv.app.data.repository.IptvRepository
import com.visiontv.app.util.AppLogger
import com.visiontv.app.util.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TvViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = IptvRepository()
    private val preferences = PreferencesManager(application)

    private val _uiState = MutableStateFlow(TvUiState(activeCategory = "Argentina"))
    val uiState: StateFlow<TvUiState> = _uiState.asStateFlow()

    init {
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            combine(
                preferences.playlistsFlow,
                preferences.favoritesFlow,
                preferences.recentsFlow
            ) { playlists, favorites, recents ->
                Triple(playlists, favorites, recents)
            }.collect { (playlists, favorites, recentUrls) ->
                _uiState.update { currentState ->
                    val recentChannels = recentUrls.mapNotNull { url -> 
                        currentState.channels.find { it.url == url } 
                    }
                    currentState.copy(
                        playlists = playlists,
                        favorites = favorites,
                        recentChannels = recentChannels
                    )
                }
                
                if (_uiState.value.channels.isEmpty()) {
                    refreshChannels()
                } else {
                    applyFilters()
                }
            }
        }
    }

    fun refreshChannels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            AppLogger.info("Loading playlists...", listOf("tv", "playlist"))
            
            runCatching {
                repository.fetchAllPlaylists(_uiState.value.playlists)
            }.onSuccess { channels ->
                AppLogger.info("${channels.size} channels loaded", listOf("tv", "playlist"))
                _uiState.update { currentState ->
                    val categories = buildCategories(channels)
                    currentState.copy(
                        isLoading = false,
                        channels = channels,
                        categories = categories
                    )
                }
                applyFilters()
            }.onFailure { error ->
                AppLogger.error("Error loading channels: ${error.message}", listOf("tv", "error"))
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = error.message ?: "Error loading channels"
                    ) 
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            val current = _uiState.value.favorites
            val updated = if (current.contains(channel.url)) current - channel.url
            else current + channel.url
            preferences.saveFavorites(updated)
        }
    }

    fun selectChannel(channel: Channel) {
        viewModelScope.launch {
            preferences.addRecent(channel.url)
            _uiState.update { it.copy(selectedChannel = channel) }
        }
    }

    fun clearSelectedChannel() {
        _uiState.update { it.copy(selectedChannel = null) }
    }

    private fun applyFilters() {
        _uiState.update { state ->
            var filtered = when (state.activeCategory) {
                "Argentina" -> state.channels.filter { isArgentina(it) }
                "Other" -> state.channels.filter { !isArgentina(it) }
                "Favorites" -> state.channels.filter { state.favorites.contains(it.url) }
                "All" -> state.channels
                else -> state.channels.filter {
                    repository.getBaseCategory(it.category) == state.activeCategory
                }
            }
            if (state.searchQuery.isNotBlank()) {
                val q = state.searchQuery.trim().lowercase()
                filtered = filtered.filter { it.name.lowercase().contains(q) }
            }
            state.copy(filteredChannels = filtered)
        }
    }

    private fun isArgentina(channel: Channel): Boolean {
        return channel.country?.uppercase() == "AR" || 
               channel.name.contains("Argentina", ignoreCase = true) ||
               channel.category.contains("Argentina", ignoreCase = true)
    }

    private fun buildCategories(channels: List<Channel>): List<String> =
        channels
            .map { repository.getBaseCategory(it.category) }
            .filter { it != "Argentina" } // Avoid duplication if "Argentina" is already a base category
            .distinct()
            .sorted()
            .let { listOf("Argentina", "Favorites", "Other", "All") + it }
}
