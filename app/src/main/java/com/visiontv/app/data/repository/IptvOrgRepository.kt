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
        
        // Use the master streams list for more reliable links if the country one fails
        val masterStreamsUrl = "https://raw.githubusercontent.com/iptv-org/iptv/refs/heads/master/streams/ar.m3u"
        
        val channels = runCatching {
            iptvRepository.fetchPlaylist(ARGENTINA_M3U_URL)
        }.getOrElse { emptyList() }

        val backupChannels = runCatching {
            iptvRepository.fetchPlaylist(masterStreamsUrl)
        }.getOrElse { emptyList() }

        (channels + backupChannels)
            .distinctBy { it.url }
            .map { it.copy(country = "AR") }
    }
}
