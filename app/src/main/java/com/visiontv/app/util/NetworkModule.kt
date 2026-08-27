package com.visiontv.app.util

import com.visiontv.app.data.remote.ArchiveOrgService
import com.visiontv.app.data.remote.PublicDomainCatalogService
import com.visiontv.app.data.remote.TmdbService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val tmdbRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val archiveRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://archive.org/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val githubRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val tmdbService: TmdbService by lazy {
        tmdbRetrofit.create(TmdbService::class.java)
    }

    val archiveService: ArchiveOrgService by lazy {
        archiveRetrofit.create(ArchiveOrgService::class.java)
    }

    val catalogService: PublicDomainCatalogService by lazy {
        githubRetrofit.create(PublicDomainCatalogService::class.java)
    }
}
