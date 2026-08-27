package com.visiontv.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.visiontv.app.data.model.CatalogItem
import com.visiontv.app.data.model.Movie
import com.visiontv.app.data.model.PlaylistSourceType
import com.visiontv.app.data.model.PublicDomainCatalog
import com.visiontv.app.data.model.Series
import com.visiontv.app.data.remote.ArchiveOrgService
import com.visiontv.app.data.remote.PublicDomainCatalogService
import com.visiontv.app.util.AppLogger
import com.visiontv.app.util.NetworkModule
import com.visiontv.app.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class PublicDomainRepository(
    private val context: Context,
    private val catalogService: PublicDomainCatalogService = NetworkModule.catalogService,
    private val archiveService: ArchiveOrgService = NetworkModule.archiveService,
    private val tmdbRepository: TmdbRepository = TmdbRepository(),
    private val preferences: PreferencesManager = PreferencesManager(context),
) {
    private val gson = Gson()
    private val streamUrlCache = mutableMapOf<String, String>()

    suspend fun getPublicDomainContent(): Pair<List<Movie>, List<Series>> = coroutineScope {
        val catalog = fetchCatalog()
        val movies = mutableListOf<Movie>()
        val seriesList = mutableListOf<Series>()

        AppLogger.info("Processing ${catalog.items.size} public domain items", listOf("public_domain"))

        catalog.items.map { item ->
            async {
                val streamUrl = resolveStreamUrl(item.archiveOrgId)
                if (streamUrl != null) {
                    if (item.type == "movie") {
                        enrichMovie(item, streamUrl).let { synchronized(movies) { movies.add(it) } }
                    } else if (item.type == "series") {
                        enrichSeries(item).let { synchronized(seriesList) { seriesList.add(it) } }
                    }
                }
            }
        }.awaitAll()

        Pair(movies, seriesList)
    }

    private suspend fun fetchCatalog(): PublicDomainCatalog = withContext(Dispatchers.IO) {
        val cache = preferences.getCatalogCache()
        val cachedJson = cache.first
        val eTag = cache.second
        val timestamp = cache.third
        val isStale = (System.currentTimeMillis() - timestamp) > TimeUnit.DAYS.toMillis(1)

        if ((!isStale) && (cachedJson != null)) {
            return@withContext gson.fromJson(cachedJson, PublicDomainCatalog::class.java)
        }

        runCatching {
            val response = catalogService.getCatalog(eTag)
            if (response.isSuccessful) {
                val newCatalog = response.body()
                val newETag = response.headers()["ETag"]
                if (newCatalog != null) {
                    val json = gson.toJson(newCatalog)
                    preferences.saveCatalogCache(json, newETag, System.currentTimeMillis())
                    return@withContext newCatalog
                }
            } else if (response.code() == 304 && cachedJson != null) {
                preferences.saveCatalogCache(cachedJson, eTag, System.currentTimeMillis())
                return@withContext gson.fromJson(cachedJson, PublicDomainCatalog::class.java)
            }
            throw Exception("Failed to fetch remote catalog")
        }.getOrElse {
            AppLogger.warning("Remote catalog fetch failed, falling back to local: ${it.message}", listOf("public_domain"))
            cachedJson?.let { json -> gson.fromJson(json, PublicDomainCatalog::class.java) } ?: fetchBundledCatalog()
        }
    }

    private fun fetchBundledCatalog(): PublicDomainCatalog {
        return try {
            context.assets.open("public_domain_catalog.json").bufferedReader().use {
                gson.fromJson(it, PublicDomainCatalog::class.java)
            }
        } catch (e: Exception) {
            AppLogger.error("Failed to load bundled catalog", listOf("public_domain"), e)
            PublicDomainCatalog(1, emptyList())
        }
    }

    private suspend fun resolveStreamUrl(identifier: String): String? {
        streamUrlCache[identifier]?.let { return it }

        return runCatching {
            val metadata = archiveService.getMetadata(identifier)
            // Prefer .mp4, then .ogv, then .mov
            val playableFile = metadata.files.firstOrNull { 
                it.name.endsWith(".mp4", ignoreCase = true) 
            } ?: metadata.files.firstOrNull { 
                it.name.endsWith(".ogv", ignoreCase = true) 
            } ?: metadata.files.firstOrNull { 
                it.name.endsWith(".mov", ignoreCase = true) 
            }
            
            playableFile?.let {
                val url = "https://archive.org/download/$identifier/${it.name}"
                streamUrlCache[identifier] = url
                url
            }
        }.onFailure {
            AppLogger.error("Failed to resolve Archive.org URL for $identifier", listOf("public_domain"), it)
        }.getOrNull()
    }

    private suspend fun enrichMovie(item: CatalogItem, streamUrl: String): Movie {
        val tmdbId = item.tmdbId
        val details = tmdbId?.let { tmdbRepository.getMovieDetails(it) }
        val search = if (details == null) tmdbRepository.getMovieInfo(item.title) else null

        val title = details?.title ?: search?.title ?: item.title
        val posterPath = details?.posterPath ?: search?.posterPath
        val backdropPath = details?.backdropPath ?: search?.backdropPath
        val overview = details?.overview ?: search?.overview ?: ""
        val voteAverage = details?.voteAverage ?: search?.voteAverage
        val releaseDate = details?.releaseDate ?: search?.releaseDate

        return Movie(
            id = "pd_${item.archiveOrgId}",
            title = title,
            streamUrl = streamUrl,
            posterUrl = tmdbRepository.getPosterUrl(posterPath),
            backdropUrl = tmdbRepository.getBackdropUrl(backdropPath),
            overview = overview,
            rating = voteAverage,
            releaseYear = item.year?.toString() ?: releaseDate?.take(4),
            category = "Public Domain",
            sourceType = PlaylistSourceType.PUBLIC_DOMAIN
        )
    }

    private suspend fun enrichSeries(item: CatalogItem): Series {
        val tmdbId = item.tmdbId
        val details = tmdbId?.let { tmdbRepository.getSeriesDetails(it) }
        val search = if (details == null) tmdbRepository.getSeriesInfo(item.title) else null

        val title = details?.name ?: search?.name ?: item.title
        val posterPath = details?.posterPath ?: search?.posterPath
        val backdropPath = details?.backdropPath ?: search?.backdropPath
        val overview = details?.overview ?: search?.overview ?: ""
        val voteAverage = details?.voteAverage ?: search?.voteAverage

        return Series(
            id = "pd_${item.archiveOrgId}",
            title = title,
            posterUrl = tmdbRepository.getPosterUrl(posterPath),
            backdropUrl = tmdbRepository.getBackdropUrl(backdropPath),
            overview = overview,
            rating = voteAverage,
            category = "Public Domain",
            sourceType = PlaylistSourceType.PUBLIC_DOMAIN,
            seasons = emptyMap()
        )
    }
}
