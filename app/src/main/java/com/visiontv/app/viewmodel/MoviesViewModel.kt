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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    private var loadJob: kotlinx.coroutines.Job? = null

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
                val moviesPlaylists = playlists.filter { it.type == PlaylistSourceType.MOVIES }
                val playlistsChanged = moviesPlaylists != _uiState.value.playlists.filter { it.type == PlaylistSourceType.MOVIES }
                
                _uiState.update { it.copy(favorites = favorites, playlists = playlists) }
                
                if ((_uiState.value.movies.isEmpty() && !uiState.value.isLoading) || playlistsChanged) {
                    loadMovies(moviesPlaylists)
                } else {
                    applyFilters()
                }
            }
        }
    }

    private fun loadMovies(playlists: List<PlaylistSource>) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
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
            
            // Process in chunks of 10 for better stability
            movies.chunked(10).forEach { chunk ->
                val enrichedChunk = coroutineScope {
                    chunk.map { movie ->
                        async {
                            runCatching { movieRepository.enrichMovie(movie) }.getOrDefault(movie)
                        }
                    }.awaitAll()
                }
                
                _uiState.update { state ->
                    val updatedAll = state.movies.map { existing ->
                        enrichedChunk.find { it.id == existing.id } ?: existing
                    }
                    state.copy(movies = updatedAll)
                }
                applyFilters()
                delay(300.milliseconds) // Slightly longer rate limit for stability
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
            val updated = if (current.contains(movie.id)) current - movie.id
                          else current + movie.id
            preferences.saveFavorites(updated)
        }
    }

    fun selectMovie(movie: Movie) {
        if (movie.streamUrl.isBlank() && movie.id.startsWith("pd_")) {
            resolveAndSelectMovie(movie)
        } else {
            _uiState.update { it.copy(selectedMovie = movie) }
        }
    }

    private fun resolveAndSelectMovie(movie: Movie) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val archiveId = movie.id.removePrefix("pd_")
            val realUrl = movieRepository.resolvePublicDomainUrl(archiveId)
            
            _uiState.update { it.copy(isLoading = false) }
            if (realUrl != null) {
                _uiState.update { it.copy(selectedMovie = movie.copy(streamUrl = realUrl)) }
            } else {
                _uiState.update { it.copy(errorMessage = "Failed to resolve video link") }
            }
        }
    }

    fun clearSelectedMovie() {
        _uiState.update { it.copy(selectedMovie = null) }
    }

    private fun applyFilters() {
        _uiState.update { state ->
            val isSearching = state.searchQuery.isNotBlank()
            
            var filtered = if (isSearching) {
                state.movies
            } else {
                if (state.activeCategory == "All") state.movies
                else state.movies.filter { it.category == state.activeCategory }
            }
            
            if (isSearching) {
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
