package com.visiontv.app.viewmodel

import com.visiontv.app.data.model.Channel
import com.visiontv.app.data.model.Movie
import com.visiontv.app.data.model.Series

data class FavoritesUiState(
    val favoriteChannels: List<Channel> = emptyList(),
    val favoriteMovies: List<Movie> = emptyList(),
    val favoriteSeries: List<Series> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
