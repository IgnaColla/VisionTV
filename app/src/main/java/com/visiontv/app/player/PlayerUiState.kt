package com.visiontv.app.player

data class PlayerUiState(
    val title: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val showControls: Boolean = false,
    val errorMessage: String? = null,
    val isEnded: Boolean = false,
    val volume: Float = 1.0f,
    val isFavorite: Boolean = false,
    val availableResolutions: List<VideoResolution> = emptyList(),
    val selectedResolution: VideoResolution? = null
)

data class VideoResolution(
    val id: String,
    val label: String,
    val width: Int,
    val height: Int,
    val bitrate: Int
)
