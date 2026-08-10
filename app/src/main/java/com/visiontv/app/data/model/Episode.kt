package com.visiontv.app.data.model

data class Episode(
    val id: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val streamUrl: String,
    val headers: Map<String, String> = emptyMap()
)
