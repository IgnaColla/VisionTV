package com.visiontv.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.visiontv.app.data.model.Movie
import com.visiontv.app.data.model.PlaylistSource
import com.visiontv.app.data.model.PlaylistSourceType
import com.visiontv.app.data.repository.MovieRepository
import com.visiontv.app.data.repository.PublicDomainRepository
import com.visiontv.app.util.AppLogger
import com.visiontv.app.util.PreferencesManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class MoviesViewModel(application: Application) : AndroidViewModel(application) {

    private val movieRepository = MovieRepository(
        publicDomainRepository = PublicDomainRepository(application)
    )
    private val preferences = PreferencesManager(application)

    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferences.playlistsFlow,
                preferences.favoritesFlow
            ) { playlists, favorites ->
                Pair(playlists, favorites)
            }.collect { (playlists, favorites) ->
                _uiState.update { it.copy(favorites = favorites) }
                if (_uiState.value.movies.isEmpty()) {
                    loadMovies(playlists.filter { it.type == PlaylistSourceType.MOVIES })
                } else {
                    applyFilters()
                }
            }
        }
    }

    private fun loadMovies(playlists: List<PlaylistSource>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                movieRepository.getMovies(playlists)
            }.onSuccess { movies ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        movies = movies,
                        categories = buildCategories(movies)
                    )
                }
                applyFilters()
                enrichMovies(movies)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Error loading movies"
                    )
                }
            }
        }
    }

    private fun enrichMovies(movies: List<Movie>) {
        viewModelScope.launch {
            AppLogger.info("Enriching ${movies.size} movies with TMDB metadata...", listOf("movies"))
            movies.forEach { movie ->
                val enriched = movieRepository.enrichMovie(movie)
                if (enriched != movie) {
                    _uiState.update { state ->
                        val updatedAll = state.movies.map {
                            if (it.id == movie.id) enriched else it
                        }
                        state.copy(movies = updatedAll)
                    }
                    applyFilters()
                }
                // Rate limiting to avoid hitting API limits
                delay(100.milliseconds)
            }
        }
    }

    fun refreshMovies() {
        viewModelScope.launch {
            preferences.playlistsFlow.collect { playlists ->
                loadMovies(playlists.filter { it.type == PlaylistSourceType.MOVIES })
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

    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch {
            val current = _uiState.value.favorites
            val updated = if (current.contains(movie.streamUrl)) current - movie.streamUrl
                          else current + movie.streamUrl
            preferences.saveFavorites(updated)
        }
    }

    fun selectMovie(movie: Movie) {
        _uiState.update { it.copy(selectedMovie = movie) }
    }

    fun clearSelectedMovie() {
        _uiState.update { it.copy(selectedMovie = null) }
    }

    private fun applyFilters() {
        _uiState.update { state ->
            var filtered = if (state.activeCategory == "All") state.movies
            else state.movies.filter { it.category == state.activeCategory }
            
            if (state.searchQuery.isNotBlank()) {
                val q = state.searchQuery.trim().lowercase()
                filtered = filtered.filter { it.title.lowercase().contains(q) }
            }
            state.copy(filteredMovies = filtered)
        }
    }

    private fun buildCategories(movies: List<Movie>): List<String> =
        listOf("All") + movies
            .map { it.category }
            .distinct()
            .sorted()
}
