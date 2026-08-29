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
        
        // Exact match with common browser headers
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"

        // High-performance OkHttp data source (reusing the app's optimized client)
        val httpDataSourceFactory = OkHttpDataSource.Factory(NetworkModule.httpClient)
            .setUserAgent(userAgent)
            .setDefaultRequestProperties(defaultRequestProperties)

        val headerDataSourceFactory = HeaderDataSourceFactory(httpDataSourceFactory)

        // Wrap it in a DefaultDataSource.Factory to support multiple protocols
        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, headerDataSourceFactory)

        val player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
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
