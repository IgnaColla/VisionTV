package com.visiontv.app.data.repository

import com.visiontv.app.data.model.Channel
import com.visiontv.app.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IptvOrgRepository(
    private val iptvRepository: IptvRepository = IptvRepository()
) {
    companion object {
        private const val ARGENTINA_M3U_URL = "https://iptv-org.github.io/iptv/countries/ar.m3u"
    }

    suspend fun getArgentinaChannels(): List<Channel> = withContext(Dispatchers.IO) {
        AppLogger.info("Loading official Argentina channel list from iptv-org...", listOf("iptv-org"))
        runCatching {
            iptvRepository.fetchPlaylist(ARGENTINA_M3U_URL)
        }.onSuccess { channels ->
            AppLogger.info("Loaded ${channels.size} channels from official AR list", listOf("iptv-org"))
        }.getOrElse {
            AppLogger.error("Failed to load official Argentina list: ${it.message}", listOf("iptv-org"), it)
            emptyList()
        }.map { channel ->
            // Ensure AR channels are tagged correctly so they show in the Argentina section
            if (channel.country.isNullOrBlank()) {
                channel.copy(country = "AR")
            } else {
                channel
            }
        }
    }
}
