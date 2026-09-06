package com.visiontv.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.visiontv.app.util.NetworkModule

class VisionTVApplication : Application(), SingletonImageLoader.Factory {

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                // Use our optimized OkHttp client for images too
                add(OkHttpNetworkFetcherFactory(NetworkModule.httpClient))
            }
            .build()
    }
}
