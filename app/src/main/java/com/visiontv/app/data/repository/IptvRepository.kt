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
        
        // Support for local files (paths starting with / or file://)
        if (url.startsWith("/") || url.startsWith("file://")) {
            return@withContext runCatching {
                val cleanPath = url.removePrefix("file://")
                val file = java.io.File(cleanPath)
                if (!file.exists()) throw java.io.FileNotFoundException("File not found: $cleanPath")
                val content = file.readText()
                val channels = parser.parse(content)
                AppLogger.info("Read ${channels.size} channels from local file $cleanPath", listOf("playlist"))
                channels
            }.getOrElse {
                AppLogger.error("Error reading local file $url: ${it.message}", listOf("playlist", "error"))
                throw it
            }
        }

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
        // Use a dedicated client for validation with optimized timeouts
        val validatorClient = httpClient.newBuilder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
            
        val requestBuilder = Request.Builder().url(url).head()
        
        // Add common headers for validation
        requestBuilder.header("User-Agent", headers["User-Agent"] ?: NetworkModule.DEFAULT_USER_AGENT)
        requestBuilder.header("Accept", "*/*")
        headers.forEach { (k, v) -> if (k != "User-Agent") requestBuilder.header(k, v) }
        
        val result = runCatching {
            validatorClient.newCall(requestBuilder.build()).execute().use { response ->
                // HTTP 2xx or 405 (HEAD not allowed)
                // We no longer accept 403 or 404 as "likely alive"
                response.isSuccessful || response.code == 405
            }
        }.getOrDefault(false)

        if (result) return@withContext true

        // If HEAD fails, try a small GET (some servers block HEAD completely)
        // Some servers return 403 on HEAD but 200 on GET with Range
        val getBuilder = Request.Builder()
            .url(url)
            .header("Range", "bytes=0-1023") // Request 1KB to be sure
            .header("User-Agent", headers["User-Agent"] ?: NetworkModule.DEFAULT_USER_AGENT)
            .header("Accept", "*/*")
            .get()
        
        headers.forEach { (k, v) -> if (k != "User-Agent") getBuilder.header(k, v) }

        runCatching {
            validatorClient.newCall(getBuilder.build()).execute().use { response ->
                // 200 (Success) or 206 (Partial Content)
                response.isSuccessful || response.code == 206
            }
        }.getOrDefault(false)
    }
}
