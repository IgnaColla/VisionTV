package com.visiontv.app.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbService {
    @GET("search/movie")
    suspend fun searchMovie(
        @Query("api_key") apiKey: String,
        @Query("query") query: String
    ): TmdbSearchResponse

    @GET("search/tv")
    suspend fun searchSeries(
        @Query("api_key") apiKey: String,
        @Query("query") query: String
    ): TmdbSearchResponse

    // Alternative using Bearer Token (Authorization header)
    @GET("search/movie")
    suspend fun searchMovieWithToken(
        @Header("Authorization") token: String,
        @Query("query") query: String
    ): TmdbSearchResponse

    @GET("search/tv")
    suspend fun searchSeriesWithToken(
        @Header("Authorization") token: String,
        @Query("query") query: String
    ): TmdbSearchResponse

    @GET("movie/{id}")
    suspend fun getMovieDetails(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String
    ): TmdbMovieDetails

    @GET("movie/{id}")
    suspend fun getMovieDetailsWithToken(
        @Path("id") id: Int,
        @Header("Authorization") token: String
    ): TmdbMovieDetails

    @GET("tv/{id}")
    suspend fun getSeriesDetails(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String
    ): TmdbSeriesDetails

    @GET("tv/{id}")
    suspend fun getSeriesDetailsWithToken(
        @Path("id") id: Int,
        @Header("Authorization") token: String
    ): TmdbSeriesDetails
}

data class TmdbSearchResponse(
    val results: List<TmdbMovie>
)

data class TmdbMovieDetails(
    val id: Int,
    val title: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    val overview: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("release_date") val releaseDate: String?,
    val runtime: Int?,
    val genres: List<TmdbGenre>?
)

data class TmdbSeriesDetails(
    val id: Int,
    val name: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    val overview: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("episode_run_time") val episodeRunTime: List<Int>?,
    val genres: List<TmdbGenre>?
)

data class TmdbGenre(
    val id: Int,
    val name: String
)

data class TmdbMovie(
    val id: Int,
    val title: String?,
    val name: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    val overview: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?
)
