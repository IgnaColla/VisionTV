package com.visiontv.app.data.model

import com.google.gson.annotations.SerializedName

data class PublicDomainCatalog(
    @SerializedName("catalog_version") val version: Int,
    @SerializedName("items") val items: List<CatalogItem>
)

data class CatalogItem(
    @SerializedName("title") val title: String,
    @SerializedName("type") val type: String, // "movie" or "series"
    @SerializedName("archive_org_id") val archiveOrgId: String,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("tmdb_id") val tmdbId: Int? = null
)
