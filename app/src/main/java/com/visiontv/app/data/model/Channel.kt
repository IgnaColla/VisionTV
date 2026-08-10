package com.visiontv.app.data.model

data class Channel(
    val id: String,
    val name: String,
    val url: String,
    val category: String = "General",
    val country: String? = null,
    val logoUrl: String? = null,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val tvgChno: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val metadata: MediaMetadata? = null
)

data class MediaMetadata(
    val overview: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val rating: Double? = null,
    val releaseDate: String? = null
)
