package com.visiontv.app.data.model

data class Series(
    val id: String,
    val title: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val overview: String? = null,
    val rating: Double? = null,
    val genres: List<String> = emptyList(),
    val category: String = "General",
    val seasons: Map<Int, List<Episode>> = emptyMap(),
    val sourceType: PlaylistSourceType = PlaylistSourceType.SERIES
)
