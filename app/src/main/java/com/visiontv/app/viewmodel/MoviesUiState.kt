package com.visiontv.app.viewmodel

import com.visiontv.app.data.model.Movie

data class MoviesUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val movies: List<Movie> = emptyList(),
    val filteredMovies: List<Movie> = emptyList(),
    val categories: List<String> = listOf("All"),
    val activeCategory: String = "All",
    val searchQuery: String = "",
    val favorites: Set<String> = emptySet(),
    val selectedMovie: Movie? = null
)
