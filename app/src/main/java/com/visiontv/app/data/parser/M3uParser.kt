package com.visiontv.app.data.parser

import com.visiontv.app.data.model.Channel
import java.util.UUID
import java.util.regex.Pattern

class M3uParser {

    private val attributePattern = Pattern.compile("""([A-Za-z0-9\\-]+)="(.*?)"""")

    fun parse(content: String): List<Channel> {
        val lines = content
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val channels = mutableListOf<Channel>()
        var pendingInfo: String? = null
        val pendingHeaders = mutableMapOf<String, String>()

        for (line in lines) {
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    pendingInfo = line
                }

                line.startsWith("#EXTVLCOPT", ignoreCase = true) -> {
                    val opt = line.substringAfter(":").trim()
                    if (opt.startsWith("http-user-agent=", ignoreCase = true)) {
                        pendingHeaders["User-Agent"] = opt.substringAfter("=").trim()
                    } else if (opt.startsWith("http-referrer=", ignoreCase = true) || 
                               opt.startsWith("http-referer=", ignoreCase = true)) {
                        pendingHeaders["Referer"] = opt.substringAfter("=").trim()
                    }
                }

                line.startsWith("#") -> {
                    continue
                }

                pendingInfo != null -> {
                    // Check for headers in the URL itself (common in some playlists)
                    // Format: http://url|User-Agent=...&Referer=...
                    val urlParts = line.split("|")
                    val cleanUrl = urlParts[0].trim()
                    
                    if (urlParts.size > 1) {
                        urlParts[1].split("&").forEach { param ->
                            val kv = param.split("=")
                            if (kv.size == 2) {
                                val key = kv[0].trim()
                                val value = kv[1].trim()
                                when {
                                    key.equals("User-Agent", ignoreCase = true) -> pendingHeaders["User-Agent"] = value
                                    key.equals("Referer", ignoreCase = true) -> pendingHeaders["Referer"] = value
                                    key.equals("Origin", ignoreCase = true) -> pendingHeaders["Origin"] = value
                                }
                            }
                        }
                    }

                    val channel = parseChannel(pendingInfo, cleanUrl, pendingHeaders.toMap())
                    if (channel != null) channels.add(channel)
                    pendingInfo = null
                    pendingHeaders.clear()
                }
            }
        }

        return channels
            .distinctBy { it.url }
    }

    private fun parseChannel(extinf: String, url: String, headers: Map<String, String>): Channel? {
        if (url.isBlank()) return null

        var title = extractTitle(extinf).ifBlank { "Unnamed Channel" }
        // Clean up title from common garbage if it's too long or contains UA strings
        if (title.contains("Chrome/") || title.contains("Safari/")) {
            title = title.substringBefore("#").substringBefore("|").trim()
        }

        val attrs = extractAttributes(extinf)

        return Channel(
            id = attrs["tvg-id"] ?: UUID.nameUUIDFromBytes(url.toByteArray()).toString(),
            name = attrs["tvg-name"] ?: title,
            url = url,
            category = attrs["group-title"] ?: "General",
            country = attrs["tvg-country"] ?: attrs["tvg-country-code"],
            logoUrl = attrs["tvg-logo"],
            tvgId = attrs["tvg-id"],
            tvgName = attrs["tvg-name"],
            tvgChno = attrs["tvg-chno"],
            headers = headers
        )
    }

    private fun extractTitle(extinf: String): String {
        val commaIndex = extinf.indexOf(',')
        return if (commaIndex >= 0 && commaIndex < extinf.length - 1) {
            extinf.substring(commaIndex + 1).trim()
        } else {
            ""
        }
    }

    private fun extractAttributes(extinf: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val matcher = attributePattern.matcher(extinf)

        while (matcher.find()) {
            val key = matcher.group(1)?.trim().orEmpty()
            val value = matcher.group(2)?.trim().orEmpty()
            if (key.isNotBlank()) {
                result[key] = value
            }
        }

        return result
    }
}
