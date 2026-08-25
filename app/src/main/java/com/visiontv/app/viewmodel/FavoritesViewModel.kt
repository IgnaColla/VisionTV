package com.visiontv.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.visiontv.app.data.model.Channel
import com.visiontv.app.data.model.Movie
import com.visiontv.app.data.model.Series
import com.visiontv.app.data.model.PlaylistSourceType
import com.visiontv.app.data.repository.IptvRepository
import com.visiontv.app.data.repository.MovieRepository
import com.visiontv.app.data.repository.SeriesRepository
import com.visiontv.app.util.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {

    private val iptvRepository = IptvRepository()
    private val movieRepository = MovieRepository()
    private val seriesRepository = SeriesRepository()
    private val preferences = PreferencesManager(application)

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val _selectedChannel = MutableStateFlow<Channel?>(null)
    val selectedChannel: StateFlow<Channel?> = _selectedChannel.asStateFlow()

    private val _selectedMovie = MutableStateFlow<Movie?>(null)
    val selectedMovie: StateFlow<Movie?> = _selectedMovie.asStateFlow()

    private val _selectedSeries = MutableStateFlow<Series?>(null)
    val selectedSeries: StateFlow<Series?> = _selectedSeries.asStateFlow()

    init {
        loadFavorites()
        
        viewModelScope.launch {
            preferences.favoritesFlow.collect {
                loadFavorites()
            }
        }
    }

    fun selectChannel(channel: Channel) {
        _selectedChannel.value = channel
    }

    fun clearChannelSelection() {
        _selectedChannel.value = null
    }

    fun selectMovie(movie: Movie) {
        _selectedMovie.value = movie
    }

    fun clearMovieSelection() {
        _selectedMovie.value = null
    }

    fun selectSeries(series: Series) {
        _selectedSeries.value = series
    }

    fun clearSeriesSelection() {
        _selectedSeries.value = null
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            val current = preferences.favoritesFlow.first()
            val updated = if (current.contains(id)) current - id else current + id
            preferences.saveFavorites(updated)
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            runCatching {
                val playlists = preferences.playlistsFlow.first()
                val favorites = preferences.favoritesFlow.first()
                
                // Fetch all data
                val allChannels = iptvRepository.fetchAllPlaylists(playlists)
                val allMovies = movieRepository.getMovies(playlists.filter { it.type == PlaylistSourceType.MOVIES })
                val allSeries = seriesRepository.getSeries(playlists.filter { it.type == PlaylistSourceType.SERIES })
                
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        favoriteChannels = allChannels.filter { favorites.contains(it.url) },
                        favoriteMovies = allMovies.filter { favorites.contains(it.streamUrl) },
                        favoriteSeries = allSeries.filter { favorites.contains(it.id) },
                    )
                }
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(isLoading = false, errorMessage = error.message ?: "Failed to load favorites")
                }
            }
        }
    }
}
