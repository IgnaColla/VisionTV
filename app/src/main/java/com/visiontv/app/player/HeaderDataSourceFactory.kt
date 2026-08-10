package com.visiontv.app.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource

@OptIn(UnstableApi::class)
class HeaderDataSourceFactory(
    private val baseFactory: HttpDataSource.Factory
) : HttpDataSource.Factory {

    private val dynamicHeaders = mutableMapOf<String, String>()

    fun setHeaders(headers: Map<String, String>) {
        dynamicHeaders.clear()
        dynamicHeaders.putAll(headers)
    }

    override fun createDataSource(): HttpDataSource {
        val dataSource = baseFactory.createDataSource()
        dynamicHeaders.forEach { (key, value) ->
            dataSource.setRequestProperty(key, value)
        }
        return dataSource
    }

    override fun setDefaultRequestProperties(defaultRequestProperties: MutableMap<String, String>): HttpDataSource.Factory {
        baseFactory.setDefaultRequestProperties(defaultRequestProperties)
        return this
    }
}
