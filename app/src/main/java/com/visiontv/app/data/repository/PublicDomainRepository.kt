package com.visiontv.app.data.repository

import android.content.Context
import com.google.gson.Gson
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
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class PublicDomainRepository(
    private val context: Context,
    private val catalogService: PublicDomainCatalogService = NetworkModule.catalogService,
    private val archiveService: ArchiveOrgService = NetworkModule.archiveService,
    private val preferences: PreferencesManager = PreferencesManager(context),
) {
    private val gson = Gson()
    private val streamUrlCache = mutableMapOf<String, String>()

    suspend fun getPublicDomainContent(): Pair<List<Movie>, List<Series>> {
        val catalog = fetchCatalog()
        
        AppLogger.info("Loading ${catalog.items.size} public domain items from catalog", listOf("public_domain"))

        val movies = catalog.items.asSequence().filter { it.type == "movie" }.map { item ->
            Movie(
                id = "pd_${item.archiveOrgId}",
                title = item.title,
                streamUrl = "", // Lazy resolution
                releaseYear = item.year?.toString(),
                category = "Public Domain",
                sourceType = PlaylistSourceType.PUBLIC_DOMAIN,
                tmdbId = item.tmdbId,
            )
        }.toList()

        val seriesList = catalog.items.asSequence().filter { it.type == "series" }.map { item ->
            Series(
                id = "pd_${item.archiveOrgId}",
                title = item.title,
                category = "Public Domain",
                sourceType = PlaylistSourceType.PUBLIC_DOMAIN,
                seasons = emptyMap(),
                tmdbId = item.tmdbId,
            )
        }.toList()

        return Pair(movies, seriesList)
    }

    suspend fun resolveStreamUrl(identifier: String): String? {
        streamUrlCache[identifier]?.let { return it }

        return runCatching {
            AppLogger.info("Resolving real stream URL for $identifier", listOf("public_domain"))
            val metadata = archiveService.getMetadata(identifier)
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
                    AppLogger.info("Fetched ${newCatalog.items.size} items from GitHub", listOf("public_domain"))
                    return@withContext newCatalog
                }
            } else if ((response.code() == 304) && (cachedJson != null)) {
                AppLogger.info("Catalog not modified (304), using cache", listOf("public_domain"))
                preferences.saveCatalogCache(cachedJson, eTag, System.currentTimeMillis())
                return@withContext gson.fromJson(cachedJson, PublicDomainCatalog::class.java)
            }
            throw Exception("Failed to fetch remote catalog: HTTP ${response.code()}")
        }.getOrElse {
            AppLogger.warning("Remote catalog fetch failed: ${it.message}", listOf("public_domain"))
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
}
