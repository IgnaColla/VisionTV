package com.visiontv.app.data.repository

import com.visiontv.app.data.model.Channel
import com.visiontv.app.data.model.Episode
import com.visiontv.app.data.model.PlaylistSource
import com.visiontv.app.data.model.Series
import com.visiontv.app.util.AppLogger

class SeriesRepository(
    private val iptvRepository: IptvRepository = IptvRepository(),
    private val tmdbRepository: TmdbRepository = TmdbRepository()
) {
    private val episodeRegex = Regex("(?i)(.*)\\s+S(\\d+)E(\\d+)(.*)")

    suspend fun getSeries(playlists: List<PlaylistSource>): List<Series> {
        val channels = iptvRepository.fetchAllPlaylists(playlists)
        val seriesMap = mutableMapOf<String, MutableList<Channel>>()

        channels.forEach { channel ->
            val match = episodeRegex.find(channel.name)
            val seriesTitle = match?.groupValues?.get(1)?.trim() ?: channel.name.trim()
            seriesMap.getOrPut(seriesTitle) { mutableListOf() }.add(channel)
        }

        return seriesMap.map { (title, episodes) ->
            val structuredSeasons = groupEpisodes(episodes)
            Series(
                id = episodes.first().id,
                title = title,
                category = episodes.first().category,
                seasons = structuredSeasons,
                posterUrl = episodes.first().logoUrl
            )
        }
    }

    suspend fun enrichSeries(series: Series): Series {
        val info = tmdbRepository.getSeriesInfo(series.title) ?: return series
        val details = tmdbRepository.getSeriesDetails(info.id)
        
        return series.copy(
            posterUrl = tmdbRepository.getPosterUrl(info.posterPath) ?: series.posterUrl,
            backdropUrl = tmdbRepository.getBackdropUrl(info.backdropPath) ?: series.backdropUrl,
            overview = info.overview ?: series.overview,
            rating = info.voteAverage ?: series.rating,
            genres = details?.genres?.map { it.name } ?: series.genres
        )
    }

    private fun groupEpisodes(channels: List<Channel>): Map<Int, List<Episode>> {
        val seasons = mutableMapOf<Int, MutableList<Episode>>()
        channels.forEach { channel ->
            val match = episodeRegex.find(channel.name)
            val seasonNum = match?.groupValues?.get(2)?.toIntOrNull() ?: 1
            val episodeNum = match?.groupValues?.get(3)?.toIntOrNull() ?: 1
            
            val episode = Episode(
                id = channel.id,
                seasonNumber = seasonNum,
                episodeNumber = episodeNum,
                title = channel.name,
                streamUrl = channel.url,
                headers = channel.headers
            )
            seasons.getOrPut(seasonNum) { mutableListOf() }.add(episode)
        }
        return seasons.mapValues { it.value.sortedBy { ep -> ep.episodeNumber } }
    }
}
