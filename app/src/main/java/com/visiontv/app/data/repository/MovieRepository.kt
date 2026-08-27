package com.visiontv.app.data.repository

import com.visiontv.app.data.model.Channel
import com.visiontv.app.data.model.Movie
import com.visiontv.app.data.model.PlaylistSource

class MovieRepository(
    private val iptvRepository: IptvRepository = IptvRepository(),
    private val tmdbRepository: TmdbRepository = TmdbRepository(),
    private val publicDomainRepository: PublicDomainRepository? = null
) {
    suspend fun getMovies(playlists: List<PlaylistSource>): List<Movie> {
        val iptvMovies = iptvRepository.fetchAllPlaylists(playlists).map { it.toMovie() }
        val pdMovies = publicDomainRepository?.getPublicDomainContent()?.first ?: emptyList()
        
        return (iptvMovies + pdMovies).distinctBy { it.id }
    }

    suspend fun resolvePublicDomainUrl(archiveId: String): String? {
        return publicDomainRepository?.resolveStreamUrl(archiveId)
    }

    suspend fun enrichMovie(movie: Movie): Movie {
        val tmdbId = movie.tmdbId
        val info = if (tmdbId != null) null else tmdbRepository.getMovieInfo(movie.title)
        val details = tmdbId?.let { tmdbRepository.getMovieDetails(it) } ?: info?.let { tmdbRepository.getMovieDetails(it.id) }
        
        val posterPath = details?.posterPath ?: info?.posterPath
        val backdropPath = details?.backdropPath ?: info?.backdropPath
        val overview = details?.overview ?: info?.overview ?: movie.overview
        val voteAverage = details?.voteAverage ?: info?.voteAverage ?: movie.rating
        val releaseDate = details?.releaseDate ?: info?.releaseDate
        
        return movie.copy(
            posterUrl = tmdbRepository.getPosterUrl(posterPath) ?: movie.posterUrl,
            backdropUrl = tmdbRepository.getBackdropUrl(backdropPath) ?: movie.backdropUrl,
            overview = overview,
            rating = voteAverage,
            releaseYear = releaseDate?.split("-")?.firstOrNull() ?: movie.releaseYear,
            runtimeMinutes = details?.runtime ?: movie.runtimeMinutes,
            genres = details?.genres?.map { it.name } ?: movie.genres
        )
    }

    private fun Channel.toMovie(): Movie {
        return Movie(
            id = id,
            title = name,
            streamUrl = url,
            headers = headers,
            category = category,
            posterUrl = logoUrl
        )
    }
}
