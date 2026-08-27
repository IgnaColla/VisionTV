package com.visiontv.app.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

interface ArchiveOrgService {
    @GET("metadata/{identifier}")
    suspend fun getMetadata(@Path("identifier") identifier: String): ArchiveMetadata
}

data class ArchiveMetadata(
    @SerializedName("files") val files: List<ArchiveFile>
)

data class ArchiveFile(
    @SerializedName("name") val name: String,
    @SerializedName("format") val format: String,
    @SerializedName("size") val size: Long? = null
)
