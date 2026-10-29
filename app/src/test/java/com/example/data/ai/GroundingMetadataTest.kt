package com.example.data.ai

import com.example.ui.viewmodel.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for grounding metadata mapping logic.
 * These tests verify the extraction of source titles and URIs from simulated metadata structures.
 */
class GroundingMetadataTest {

    // Helper data classes to simulate SDK structures for testing extraction logic
    data class FakeWeb(val title: String, val uri: String)
    data class FakeChunk(val web: FakeWeb?)
    data class FakeMetadata(val groundingChunks: List<FakeChunk>?)

    @Test
    fun `test source mapping with valid metadata`() {
        val fakeChunks = listOf(
            FakeChunk(FakeWeb("Google Search", "https://google.com")),
            FakeChunk(FakeWeb("Financial Times", "https://ft.com"))
        )
        
        val sources = mutableListOf<Source>()
        fakeChunks.forEach { chunk ->
            chunk.web?.let { web ->
                if (web.uri.isNotBlank() && web.title.isNotBlank()) {
                    sources.add(Source(web.title, web.uri))
                }
            }
        }
        
        assertEquals(2, sources.size)
        assertEquals("Google Search", sources[0].title)
        assertEquals("https://google.com", sources[0].url)
        assertEquals("Financial Times", sources[1].title)
        assertEquals("https://ft.com", sources[1].url)
    }

    @Test
    fun `test source mapping with missing fields`() {
        val fakeChunks = listOf(
            FakeChunk(FakeWeb("", "https://google.com")), // Missing title
            FakeChunk(FakeWeb("Broken Link", "")),      // Missing URI
            FakeChunk(null)                             // Null web
        )
        
        val sources = mutableListOf<Source>()
        fakeChunks.forEach { chunk ->
            chunk.web?.let { web ->
                if (web.uri.isNotBlank() && web.title.isNotBlank()) {
                    sources.add(Source(web.title, web.uri))
                }
            }
        }
        
        assertTrue(sources.isEmpty())
    }

    @Test
    fun `test duplicate source handling`() {
        val rawSources = listOf(
            Source("Google", "https://google.com"),
            Source("Google", "https://google.com"),
            Source("Other", "https://other.com")
        )
        
        val distinctSources = rawSources.distinctBy { it.url }
        
        assertEquals(2, distinctSources.size)
        assertEquals("Google", distinctSources[0].title)
        assertEquals("Other", distinctSources[1].title)
    }
}
