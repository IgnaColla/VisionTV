// app/src/main/java/com/visiontv/app/data/model/PlaylistSource.kt
package com.visiontv.app.data.model

enum class PlaylistSourceType {
    LIVE_TV,
    MOVIES,
    SERIES
}

data class PlaylistSource(
    val name: String,
    val url: String,
    val type: PlaylistSourceType = PlaylistSourceType.LIVE_TV
)