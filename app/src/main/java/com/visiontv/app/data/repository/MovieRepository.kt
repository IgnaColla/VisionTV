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
        
        return (iptvMovies + pdMovies).distinctBy { it.streamUrl }
    }

    suspend fun enrichMovie(movie: Movie): Movie {
        val info = tmdbRepository.getMovieInfo(movie.title) ?: return movie
        val details = tmdbRepository.getMovieDetails(info.id)
        
        return movie.copy(
            posterUrl = tmdbRepository.getPosterUrl(info.posterPath) ?: movie.posterUrl,
            backdropUrl = tmdbRepository.getBackdropUrl(info.backdropPath) ?: movie.backdropUrl,
            overview = info.overview ?: movie.overview,
            rating = info.voteAverage ?: movie.rating,
            releaseYear = info.releaseDate?.split("-")?.firstOrNull() ?: movie.releaseYear,
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
