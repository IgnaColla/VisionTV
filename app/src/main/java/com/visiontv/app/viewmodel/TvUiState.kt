package com.visiontv.app.viewmodel

import com.visiontv.app.data.model.Channel
import com.visiontv.app.data.model.PlaylistSource

data class TvUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val channels: List<Channel> = emptyList(),
    val filteredChannels: List<Channel> = emptyList(),
    val recentChannels: List<Channel> = emptyList(),
    val recentUrls: List<String> = emptyList(),
    val categories: List<String> = listOf("Argentina", "Favorites", "Other", "All"),
    val activeCategory: String = "Argentina",
    val searchQuery: String = "",
    val favorites: Set<String> = emptySet(),
    val playlists: List<PlaylistSource> = emptyList(),
    val selectedChannel: Channel? = null
)
