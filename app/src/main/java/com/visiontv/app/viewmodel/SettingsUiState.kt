package com.visiontv.app.viewmodel

import com.visiontv.app.data.model.PlaylistSource
import com.visiontv.app.data.model.PlaylistSourceType

data class SettingsUiState(
    val playlists: List<PlaylistSource> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showAddDialog: Boolean = false,
    val newPlaylistName: String = "",
    val newPlaylistUrl: String = "",
    val newPlaylistType: PlaylistSourceType = PlaylistSourceType.LIVE_TV
)
