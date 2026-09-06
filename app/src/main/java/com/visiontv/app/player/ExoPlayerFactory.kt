package com.visiontv.app.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.visiontv.app.util.NetworkModule

object ExoPlayerFactory {

    @OptIn(UnstableApi::class)
    fun create(context: Context): Pair<ExoPlayer, HeaderDataSourceFactory> {
        val defaultRequestProperties = mutableMapOf<String, String>()
        defaultRequestProperties["Accept"] = "*/*"
        defaultRequestProperties["Connection"] = "keep-alive"
        
        // Use optimized User-Agent and remove redundant hardcoded Origin/Referer
        val userAgent = NetworkModule.DEFAULT_USER_AGENT

        // High-performance OkHttp data source
        val httpDataSourceFactory = OkHttpDataSource.Factory(NetworkModule.httpClient)
            .setUserAgent(userAgent)
            .setDefaultRequestProperties(defaultRequestProperties)

        val headerDataSourceFactory = HeaderDataSourceFactory(httpDataSourceFactory)

        // Wrap it in a DefaultDataSource.Factory to support multiple protocols
        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, headerDataSourceFactory)

        val player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(dataSourceFactory)
                    .setLiveTargetOffsetMs(8000)
            )
            .setLivePlaybackSpeedControl(
                androidx.media3.exoplayer.DefaultLivePlaybackSpeedControl.Builder()
                    .setFallbackMaxPlaybackSpeed(1.04f)
                    .build()
            )
            .setLoadControl(
                androidx.media3.exoplayer.DefaultLoadControl.Builder()
                    .setBufferDurationsMs(15000, 50000, 2500, 5000)
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()
            )
            .build().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true
                )
                playWhenReady = true
            }
        
        return Pair(player, headerDataSourceFactory)
    }
}
