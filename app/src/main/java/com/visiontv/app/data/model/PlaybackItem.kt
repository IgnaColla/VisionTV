package com.visiontv.app.data.model

enum class PlaybackType {
    LIVE_TV,
    MOVIE,
    SERIES
}

data class PlaybackItem(
    val url: String,
    val title: String,
    val type: PlaybackType,
    val headers: Map<String, String> = emptyMap()
)
