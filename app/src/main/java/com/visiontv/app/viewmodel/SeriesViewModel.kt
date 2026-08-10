package com.visiontv.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.visiontv.app.data.model.PlaylistSource
import com.visiontv.app.data.model.PlaylistSourceType
import com.visiontv.app.data.model.Series
import com.visiontv.app.data.repository.SeriesRepository
import com.visiontv.app.util.AppLogger
import com.visiontv.app.util.PreferencesManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SeriesViewModel(application: Application) : AndroidViewModel(application) {

    private val seriesRepository = SeriesRepository()
    private val preferences = PreferencesManager(application)

    private val _uiState = MutableStateFlow(SeriesUiState())
    val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferences.playlistsFlow,
                preferences.favoritesFlow
            ) { playlists, favorites ->
                Pair(playlists, favorites)
            }.collect { (playlists, favorites) ->
                _uiState.update { it.copy(favorites = favorites) }
                if (_uiState.value.series.isEmpty()) {
                    loadSeries(playlists.filter { it.type == PlaylistSourceType.SERIES })
                } else {
                    applyFilters()
                }
            }
        }
    }

    private fun loadSeries(playlists: List<PlaylistSource>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                seriesRepository.getSeries(playlists)
            }.onSuccess { seriesList ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        series = seriesList,
                        categories = buildCategories(seriesList)
                    )
                }
                applyFilters()
                enrichSeries(seriesList)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Error loading series"
                    )
                }
            }
        }
    }

    private fun enrichSeries(seriesList: List<Series>) {
        viewModelScope.launch {
            AppLogger.info("Enriching ${seriesList.size} series with TMDB metadata...", listOf("series"))
            seriesList.forEach { series ->
                val enriched = seriesRepository.enrichSeries(series)
                if (enriched != series) {
                    _uiState.update { state ->
                        val updatedAll = state.series.map {
                            if (it.id == series.id) enriched else it
                        }
                        state.copy(series = updatedAll)
                    }
                    applyFilters()
                }
                delay(100)
            }
        }
    }

    fun refreshSeries() {
        viewModelScope.launch {
            preferences.playlistsFlow.collect { playlists ->
                loadSeries(playlists.filter { it.type == PlaylistSourceType.SERIES })
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun updateCategory(category: String) {
        _uiState.update { it.copy(activeCategory = category) }
        applyFilters()
    }

    fun toggleFavorite(series: Series) {
        viewModelScope.launch {
            val current = _uiState.value.favorites
            val updated = if (current.contains(series.id)) current - series.id
                          else current + series.id
            preferences.saveFavorites(updated)
        }
    }

    fun selectSeries(series: Series) {
        _uiState.update { it.copy(selectedSeries = series) }
    }

    fun clearSelectedSeries() {
        _uiState.update { it.copy(selectedSeries = null) }
    }

    private fun applyFilters() {
        _uiState.update { state ->
            var filtered = if (state.activeCategory == "All") state.series
            else state.series.filter { it.category == state.activeCategory }
            
            if (state.searchQuery.isNotBlank()) {
                val q = state.searchQuery.trim().lowercase()
                filtered = filtered.filter { it.title.lowercase().contains(q) }
            }
            state.copy(filteredSeries = filtered)
        }
    }

    private fun buildCategories(seriesList: List<Series>): List<String> =
        listOf("All") + seriesList
            .map { it.category }
            .distinct()
            .sorted()
}
