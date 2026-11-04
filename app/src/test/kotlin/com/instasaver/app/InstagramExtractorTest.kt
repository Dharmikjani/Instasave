package com.instasaver.app

import com.instasaver.app.core.extractor.InstagramExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class InstagramExtractorTest {

    @Test
    fun testReelUrlExtraction() {
        val url = "https://www.instagram.com/reel/C7xY9Z1A2B3/?igsh=MWx5YXAzZWlndnA3"
        val code = InstagramExtractor.extractShortcode(url)
        assertNotNull(code)
        assertEquals("C7xY9Z1A2B3", code)
    }

    @Test
    fun testPostUrlExtraction() {
        val url = "https://instagram.com/p/DB12345XYZ/?utm_source=ig_web_copy_link"
        val code = InstagramExtractor.extractShortcode(url)
        assertNotNull(code)
        assertEquals("DB12345XYZ", code)
    }

    @Test
    fun testShareReelUrlExtraction() {
        val url = "https://www.instagram.com/share/reel/CQabcdef123/"
        val code = InstagramExtractor.extractShortcode(url)
        assertNotNull(code)
        assertEquals("CQabcdef123", code)
    }

    @Test
    fun testTvUrlExtraction() {
        val url = "https://www.instagram.com/tv/B_xyz987654/"
        val code = InstagramExtractor.extractShortcode(url)
        assertNotNull(code)
        assertEquals("B_xyz987654", code)
    }

    @Test
    fun testInvalidUrlExtraction() {
        val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val code = InstagramExtractor.extractShortcode(url)
        assertNull(code)

        val fbUrl = "https://www.facebook.com/reel/123456789"
        val fbCode = InstagramExtractor.extractShortcode(fbUrl)
        assertNull(fbCode)
    }
}

