package com.visiontv.app.data.repository

import com.visiontv.app.data.model.Channel
import com.visiontv.app.data.model.PlaylistSource
import com.visiontv.app.data.parser.M3uParser
import com.visiontv.app.util.AppLogger
import com.visiontv.app.util.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class IptvRepository(
    private val parser: M3uParser = M3uParser(),
    private val httpClient: OkHttpClient = NetworkModule.httpClient
) {

    suspend fun fetchPlaylist(url: String): List<Channel> = withContext(Dispatchers.IO) {
        AppLogger.info("Fetching: $url", listOf("playlist"))
        val request = Request.Builder().url(url).get().build()
        
        runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    AppLogger.error("HTTP ${response.code} — $url", listOf("playlist", "error"))
                    throw IllegalStateException("HTTP Error ${response.code}")
                }
                val body = response.body?.string()
                    ?: throw IllegalStateException("Empty response body")
                val channels = parser.parse(body)
                AppLogger.info("Parsed ${channels.size} channels from $url", listOf("playlist"))
                channels
            }
        }.getOrElse { 
            AppLogger.error("Network error fetching $url: ${it.message}", listOf("playlist", "error"))
            throw it 
        }
    }

    suspend fun fetchAllPlaylists(playlists: List<PlaylistSource>): List<Channel> = coroutineScope {
        AppLogger.info("Starting load for ${playlists.size} playlist(s)", listOf("playlist"))
        playlists.map { playlist ->
            async {
                runCatching { fetchPlaylist(playlist.url) }
                    .onFailure {
                        AppLogger.warning("Failed to load ${playlist.name}: ${it.message}", listOf("playlist"))
                    }
                    .getOrElse { emptyList() }
            }
        }.awaitAll().flatten().distinctBy { it.url }.also {
            AppLogger.info("Total: ${it.size} unique channels loaded", listOf("playlist"))
        }
    }

    fun getBaseCategory(category: String?): String {
        if (category.isNullOrBlank()) return "General"
        return category.split(":", ";", "|", "-")
            .firstOrNull()?.trim()?.ifBlank { "General" } ?: "General"
    }

    suspend fun validateChannel(url: String, headers: Map<String, String> = emptyMap()): Boolean = withContext(Dispatchers.IO) {
        // Fast HEAD request with short timeout
        val shortTimeoutClient = httpClient.newBuilder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .build()
            
        val requestBuilder = Request.Builder().url(url).head()
        headers.forEach { (k, v) -> requestBuilder.header(k, v) }
        
        val result = runCatching {
            shortTimeoutClient.newCall(requestBuilder.build()).execute().use { response ->
                response.isSuccessful || response.code == 405
            }
        }.getOrDefault(false)

        if (result) return@withContext true

        // If HEAD fails, try a small GET (some servers block HEAD)
        // Note: We don't accept 403 (Forbidden) anymore as "alive" if it doesn't play
        val getBuilder = Request.Builder().url(url).header("Range", "bytes=0-0")
        headers.forEach { (k, v) -> getBuilder.header(k, v) }

        runCatching {
            shortTimeoutClient.newCall(getBuilder.build()).execute().use { response ->
                response.isSuccessful || response.code == 206
            }
        }.getOrDefault(false)
    }
}
