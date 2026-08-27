package com.visiontv.app.data.model

data class Movie(
    val id: String,
    val title: String,
    val streamUrl: String,
    val headers: Map<String, String> = emptyMap(),
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val overview: String? = null,
    val rating: Double? = null,
    val releaseYear: String? = null,
    val runtimeMinutes: Int? = null,
    val genres: List<String> = emptyList(),
    val category: String = "General",
    val sourceType: PlaylistSourceType = PlaylistSourceType.MOVIES
)
