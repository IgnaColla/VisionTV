package com.visiontv.app.viewmodel

import com.visiontv.app.data.model.PlaylistSource
import com.visiontv.app.data.model.Series

data class SeriesUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val series: List<Series> = emptyList(),
    val filteredSeries: List<Series> = emptyList(),
    val categories: List<String> = listOf("All"),
    val activeCategory: String = "All",
    val searchQuery: String = "",
    val favorites: Set<String> = emptySet(),
    val playlists: List<PlaylistSource> = emptyList(),
    val selectedSeries: Series? = null
)
