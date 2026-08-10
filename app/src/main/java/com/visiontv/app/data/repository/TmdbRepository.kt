package com.visiontv.app.data.repository

import com.visiontv.app.BuildConfig
import com.visiontv.app.data.remote.TmdbMovie
import com.visiontv.app.data.remote.TmdbMovieDetails
import com.visiontv.app.data.remote.TmdbSeriesDetails
import com.visiontv.app.data.remote.TmdbService
import com.visiontv.app.util.AppLogger
import com.visiontv.app.util.NetworkModule

class TmdbRepository(
    private val service: TmdbService = NetworkModule.tmdbService
) {
    private val apiKey = BuildConfig.TMDB_TOKEN

    suspend fun getMovieInfo(title: String): TmdbMovie? {
        if (apiKey.isBlank()) return null
        
        val cleanTitle = cleanTitle(title)
        AppLogger.info("Searching TMDB Movie: $cleanTitle", listOf("tmdb"))
        
        return runCatching {
            val response = if (apiKey.startsWith("eyJ")) {
                service.searchMovieWithToken("Bearer $apiKey", cleanTitle)
            } else {
                service.searchMovie(apiKey, cleanTitle)
            }
            val result = response.results.firstOrNull()
            if (result != null) {
                AppLogger.info("Found TMDB Movie: ${result.title} for $cleanTitle", listOf("tmdb"))
            } else {
                AppLogger.warning("No TMDB Movie found for $cleanTitle", listOf("tmdb"))
            }
            result
        }.onFailure {
            AppLogger.error("Failed to fetch TMDB info for $cleanTitle", listOf("tmdb"), it)
        }.getOrNull()
    }

    suspend fun getSeriesInfo(title: String): TmdbMovie? {
        if (apiKey.isBlank()) return null
        
        val cleanTitle = cleanTitle(title)
        AppLogger.info("Searching TMDB Series: $cleanTitle", listOf("tmdb"))
        
        return runCatching {
            val response = if (apiKey.startsWith("eyJ")) {
                service.searchSeriesWithToken("Bearer $apiKey", cleanTitle)
            } else {
                service.searchSeries(apiKey, cleanTitle)
            }
            val result = response.results.firstOrNull()
            if (result != null) {
                AppLogger.info("Found TMDB Series: ${result.name} for $cleanTitle", listOf("tmdb"))
            } else {
                AppLogger.warning("No TMDB Series found for $cleanTitle", listOf("tmdb"))
            }
            result
        }.onFailure {
            AppLogger.error("Failed to fetch TMDB info for series $cleanTitle", listOf("tmdb"), it)
        }.getOrNull()
    }

    suspend fun getMovieDetails(id: Int): TmdbMovieDetails? {
        if (apiKey.isBlank()) return null
        return runCatching {
            if (apiKey.startsWith("eyJ")) {
                service.getMovieDetailsWithToken(id, "Bearer $apiKey")
            } else {
                service.getMovieDetails(id, apiKey)
            }
        }.getOrNull()
    }

    suspend fun getSeriesDetails(id: Int): TmdbSeriesDetails? {
        if (apiKey.isBlank()) return null
        return runCatching {
            if (apiKey.startsWith("eyJ")) {
                service.getSeriesDetailsWithToken(id, "Bearer $apiKey")
            } else {
                service.getSeriesDetails(id, apiKey)
            }
        }.getOrNull()
    }

    private fun cleanTitle(title: String): String {
        return title.split(Regex("(?i)(\\d{4}|4k|1080p|720p|hdtv|web-dl|bluray|x264|x265|aac)"))
            .first()
            .replace(".", " ")
            .replace("_", " ")
            .trim()
    }

    fun getPosterUrl(path: String?): String? {
        if (path == null) return null
        return "https://image.tmdb.org/t/p/w500$path"
    }

    fun getBackdropUrl(path: String?): String? {
        if (path == null) return null
        return "https://image.tmdb.org/t/p/original$path"
    }
}
