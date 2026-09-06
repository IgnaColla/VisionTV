package com.visiontv.app.util

import android.annotation.SuppressLint
import com.visiontv.app.data.remote.TmdbService
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object NetworkModule {
    const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

    val httpClient: OkHttpClient by lazy {
        val trustAllCerts = @SuppressLint("CustomX509TrustManager") object : X509TrustManager {
            @SuppressLint("TrustAllX509TrustManager")
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            @SuppressLint("TrustAllX509TrustManager")
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, arrayOf<TrustManager>(trustAllCerts), SecureRandom())

        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts)
            .hostnameVerifier { _, _ -> true }
            .cookieJar(object : CookieJar {
                private val cookieStore = mutableMapOf<String, List<Cookie>>()
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    cookieStore[url.host] = cookies
                }
                override fun loadForRequest(url: HttpUrl): List<Cookie> {
                    return cookieStore[url.host] ?: listOf()
                }
            })
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request()
                val newRequestBuilder = request.newBuilder()
                
                if (request.header("User-Agent") == null) {
                    newRequestBuilder.header("User-Agent", DEFAULT_USER_AGENT)
                }
                if (request.header("Accept") == null) {
                    newRequestBuilder.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                }
                if (request.header("Accept-Language") == null) {
                    newRequestBuilder.header("Accept-Language", "en-US,en;q=0.9,es;q=0.8")
                }
                if (request.header("Sec-Ch-Ua") == null) {
                    newRequestBuilder.header("Sec-Ch-Ua", "\"Not A(Brand\";v=\"99\", \"Google Chrome\";v=\"128\", \"Chromium\";v=\"128\"")
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

    val archiveService: com.visiontv.app.data.remote.ArchiveOrgService by lazy {
        archiveRetrofit.create(com.visiontv.app.data.remote.ArchiveOrgService::class.java)
    }

    val catalogService: com.visiontv.app.data.remote.PublicDomainCatalogService by lazy {
        githubRetrofit.create(com.visiontv.app.data.remote.PublicDomainCatalogService::class.java)
    }
}
