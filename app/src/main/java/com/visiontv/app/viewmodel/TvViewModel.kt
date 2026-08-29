package com.visiontv.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.visiontv.app.data.model.Channel
import com.visiontv.app.data.model.PlaylistSource
import com.visiontv.app.data.repository.IptvOrgRepository
import com.visiontv.app.data.repository.IptvRepository
import com.visiontv.app.util.AppLogger
import com.visiontv.app.util.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class TvViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = IptvRepository()
    private val iptvOrgRepository = IptvOrgRepository(repository)
    private val preferences = PreferencesManager(application)

    private val _uiState = MutableStateFlow(TvUiState(activeCategory = "All"))
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
                val currentPlaylists = _uiState.value.playlists
                val playlistsChanged = playlists != currentPlaylists
                
                _uiState.update { currentState ->
                    val recentChannels = recentUrls.mapNotNull { url -> 
                        currentState.channels.find { it.url == url } 
                    }
                    currentState.copy(
                        playlists = playlists,
                        favorites = favorites,
                        recentUrls = recentUrls,
                        recentChannels = recentChannels
                    )
                }
                
                if (_uiState.value.channels.isEmpty() || playlistsChanged) {
                    refreshChannels(playlists)
                } else {
                    applyFilters()
                }
            }
        }
    }

    fun refreshChannels(playlists: List<PlaylistSource>? = null) {
        val targetPlaylists = playlists ?: _uiState.value.playlists
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            AppLogger.info("Loading ${targetPlaylists.size} playlists...", listOf("tv", "playlist"))
            
            runCatching {
                val userChannels = repository.fetchAllPlaylists(targetPlaylists)
                val officialArgentina = iptvOrgRepository.getArgentinaChannels()
                (userChannels + officialArgentina).distinctBy { it.url }
            }.onSuccess { channels ->
                AppLogger.info("${channels.size} channels loaded", listOf("tv", "playlist"))
                _uiState.update { currentState ->
                    val categories = buildCategories(channels)
                    val recentChannels = currentState.recentUrls.mapNotNull { url ->
                        channels.find { it.url == url }
                    }
                    currentState.copy(
                        isLoading = false,
                        channels = channels,
                        categories = categories,
                        recentChannels = recentChannels
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

    fun toggleShowOnlyWorking() {
        _uiState.update { it.copy(showOnlyWorking = !it.showOnlyWorking) }
        applyFilters()
    }

    fun startCleanup() {
        if (_uiState.value.isValidating) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isValidating = true, showOnlyWorking = true) }
            val channels = _uiState.value.channels
            AppLogger.info("Starting cleanup for ${channels.size} channels...", listOf("tv", "cleanup"))
            
            // Validate in chunks
            channels.chunked(30).forEach { chunk ->
                val deadInChunk = mutableSetOf<String>()
                coroutineScope {
                    chunk.map { channel ->
                        async {
                            if (!repository.validateChannel(channel.url, channel.headers)) {
                                synchronized(deadInChunk) { deadInChunk.add(channel.url) }
                            }
                        }
                    }.awaitAll()
                }
                
                _uiState.update { it.copy(deadChannels = it.deadChannels + deadInChunk) }
                applyFilters() // Update list visually while working
            }
            
            _uiState.update { it.copy(isValidating = false) }
            AppLogger.info("Cleanup finished. Found ${_uiState.value.deadChannels.size} dead channels.", listOf("tv", "cleanup"))
        }
    }

    private fun applyFilters() {
        _uiState.update { state ->
            val isSearching = state.searchQuery.isNotBlank()
            
            val baseList = if (state.showOnlyWorking) {
                state.channels.filter { !state.deadChannels.contains(it.url) }
            } else {
                state.channels
            }
            
            var filtered = if (isSearching) {
                // Global search across all channels
                baseList
            } else {
                when (state.activeCategory) {
                    "Argentina" -> baseList.filter { isArgentina(it) }
                    "Other" -> baseList.filter { !isArgentina(it) }
                    "Favorites" -> baseList.filter { state.favorites.contains(it.url) }
                    "All" -> baseList
                    else -> baseList.filter {
                        repository.getBaseCategory(it.category) == state.activeCategory
                    }
                }
            }
            
            if (isSearching) {
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
