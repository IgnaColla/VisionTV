package com.visiontv.app.data.repository

import com.visiontv.app.data.parser.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock

class IptvRepositoryTest {

    private val parser = mock<M3uParser>()
    private val repository = IptvRepository(parser)

    @Test
    fun `getBaseCategory extracts primary category correctly`() {
        assertEquals("Movies", repository.getBaseCategory("Movies: Action"))
        assertEquals("TV", repository.getBaseCategory("TV | Sports"))
        assertEquals("General", repository.getBaseCategory(null))
        assertEquals("General", repository.getBaseCategory(""))
    }
}
