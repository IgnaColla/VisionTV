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
            .addInterceptor { chain ->
                val request = chain.request()
                val newRequestBuilder = request.newBuilder()
                
                // Only add headers if they are not already set (don't overwrite channel-specific headers)
                if (request.header("User-Agent") == null) {
                    newRequestBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                }
                if (request.header("Accept") == null) {
                    newRequestBuilder.header("Accept", "*/*")
                }
                
                chain.proceed(newRequestBuilder.build())
            }
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
