package com.visiontv.app.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class M3uParserTest {

    private val parser = M3uParser()

    @Test
    fun `parse simple m3u content returns list of channels`() {
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-id="CNN.us" tvg-name="CNN" tvg-logo="https://example.com/cnn.png" group-title="News",CNN International
            http://example.com/cnn/index.m3u8
            #EXTINF:-1 tvg-id="Discovery.us" group-title="Documentary",Discovery Channel
            http://example.com/discovery/index.m3u8
        """.trimIndent()

        val channels = parser.parse(content)

        assertEquals(2, channels.size)
        
        val cnn = channels[0]
        assertEquals("CNN.us", cnn.id)
        assertEquals("CNN", cnn.name)
        assertEquals("http://example.com/cnn/index.m3u8", cnn.url)
        assertEquals("News", cnn.category)
        assertEquals("https://example.com/cnn.png", cnn.logoUrl)

        val discovery = channels[1]
        assertEquals("Discovery.us", discovery.id)
        assertEquals("Discovery Channel", discovery.name)
        assertEquals("Documentary", discovery.category)
    }

    @Test
    fun `parse content with missing tvg attributes uses fallback`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,Generic Channel
            http://example.com/generic.m3u8
        """.trimIndent()

        val channels = parser.parse(content)

        assertEquals(1, channels.size)
        val generic = channels[0]
        assertEquals("Generic Channel", generic.name)
        assertNotNull(generic.id) // Should generate a UUID
    }

    @Test
    fun `parse empty content returns empty list`() {
        val channels = parser.parse("")
        assertEquals(0, channels.size)
    }
}
