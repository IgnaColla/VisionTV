package com.visiontv.app.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.visiontv.app.data.model.PlaylistSource
import com.visiontv.app.data.model.PlaylistSourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "visiontv_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        private val PLAYLISTS_KEY = stringPreferencesKey("iptv_playlists")
        private val FAVORITES_KEY = stringPreferencesKey("iptv_favorites")
        private val RECENTS_KEY = stringPreferencesKey("iptv_recents")
        private const val MAX_RECENTS = 10
    }

    val playlistsFlow: Flow<List<PlaylistSource>> = context.dataStore.data.map { prefs ->
        val raw = prefs[PLAYLISTS_KEY]
        if (raw.isNullOrBlank()) defaultPlaylists() else decodePlaylists(raw)
    }

    val favoritesFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[FAVORITES_KEY] ?: "[]"
        decodeStringSet(raw)
    }

    val recentsFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[RECENTS_KEY] ?: "[]"
        decodeStringList(raw)
    }

    suspend fun savePlaylists(playlists: List<PlaylistSource>) {
        context.dataStore.edit { prefs ->
            prefs[PLAYLISTS_KEY] = encodePlaylists(playlists)
        }
    }

    suspend fun saveFavorites(favorites: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[FAVORITES_KEY] = encodeStringSet(favorites)
        }
    }

    suspend fun addRecent(channelUrl: String) {
        context.dataStore.edit { prefs ->
            val current = decodeStringList(prefs[RECENTS_KEY] ?: "[]").toMutableList()
            current.remove(channelUrl) // Remove if already exists to move it to the front
            current.add(0, channelUrl) // Add to the beginning
            val trimmed = current.take(MAX_RECENTS)
            prefs[RECENTS_KEY] = encodeStringList(trimmed)
        }
    }

    private fun encodePlaylists(playlists: List<PlaylistSource>): String {
        val array = JSONArray()
        playlists.forEach {
            array.put(
                JSONObject()
                    .put("name", it.name)
                    .put("url", it.url)
                    .put("type", it.type.name),
            )
        }
        return array.toString()
    }

    private fun decodePlaylists(raw: String): List<PlaylistSource> {
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val typeStr = obj.optString("type", PlaylistSourceType.LIVE_TV.name)
                    val type = try {
                        PlaylistSourceType.valueOf(typeStr)
                    } catch (_: Exception) {
                        PlaylistSourceType.LIVE_TV
                    }
                    add(
                        PlaylistSource(
                            name = obj.optString("name"),
                            url = obj.optString("url"),
                            type = type,
                        ),
                    )
                }
            }
        }.getOrElse { defaultPlaylists() }
    }

    private fun encodeStringSet(values: Set<String>): String {
        val array = JSONArray()
        values.forEach { array.put(it) }
        return array.toString()
    }

    private fun decodeStringSet(raw: String): Set<String> {
        return runCatching {
            val array = JSONArray(raw)
            buildSet { for (i in 0 until array.length()) add(array.getString(i)) }
        }.getOrElse { emptySet() }
    }

    private fun encodeStringList(values: List<String>): String {
        val array = JSONArray()
        values.forEach { array.put(it) }
        return array.toString()
    }

    private fun decodeStringList(raw: String): List<String> {
        return runCatching {
            val array = JSONArray(raw)
            buildList { for (i in 0 until array.length()) add(array.getString(i)) }
        }.getOrElse { emptyList() }
    }

    private fun defaultPlaylists(): List<PlaylistSource> = listOf(
        PlaylistSource(
            name = "IPTV-org TV",
            url = "https://iptv-org.github.io/iptv/index.m3u",
            type = PlaylistSourceType.LIVE_TV,
        ),
        PlaylistSource(
            name = "IPTV-org Movies",
            url = "https://iptv-org.github.io/iptv/categories/movies.m3u",
            type = PlaylistSourceType.MOVIES,
        ),
        PlaylistSource(
            name = "IPTV-org Series",
            url = "https://iptv-org.github.io/iptv/categories/entertainment.m3u",
            type = PlaylistSourceType.SERIES,
        ),
    )
}
