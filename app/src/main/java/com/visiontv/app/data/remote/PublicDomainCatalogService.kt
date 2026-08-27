package com.visiontv.app.data.remote

import com.visiontv.app.data.model.PublicDomainCatalog
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface PublicDomainCatalogService {
    @GET("IgnaColla/VisionTV/main/catalog/public_domain.json")
    suspend fun getCatalog(
        @Header("If-None-Match") eTag: String? = null
    ): Response<PublicDomainCatalog>
}
