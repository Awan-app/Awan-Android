package com.awan.app.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlUtilsTest {

    @Test
    fun `resolveBackendImageUrl with relative path with leading slash strips api suffix`() {
        val result = resolveBackendImageUrl(
            imagePath = "/images/store/twilight_light_03.png",
            baseUrl = "https://backend-production-c701.up.railway.app/api/"
        )
        assertEquals("https://backend-production-c701.up.railway.app/images/store/twilight_light_03.png", result)
    }

    @Test
    fun `resolveBackendImageUrl with relative path without leading slash and baseUrl without trailing slash`() {
        val result = resolveBackendImageUrl(
            imagePath = "images/store/twilight_light_03.png",
            baseUrl = "https://backend-production-c701.up.railway.app/api"
        )
        assertEquals("https://backend-production-c701.up.railway.app/images/store/twilight_light_03.png", result)
    }

    @Test
    fun `resolveBackendImageUrl with localhost baseUrl`() {
        val result = resolveBackendImageUrl(
            imagePath = "/images/store/frame.png",
            baseUrl = "http://localhost:8080/api/"
        )
        assertEquals("http://localhost:8080/images/store/frame.png", result)
    }

    @Test
    fun `resolveBackendImageUrl with absolute http or https url returns as is`() {
        val httpUrl = "http://example.com/images/frame.png"
        val httpsUrl = "https://cdn.example.com/custom.png"

        assertEquals(httpUrl, resolveBackendImageUrl(httpUrl, "https://backend.com/api/"))
        assertEquals(httpsUrl, resolveBackendImageUrl(httpsUrl, "https://backend.com/api/"))
    }

    @Test
    fun `resolveBackendImageUrl with null or blank path returns null`() {
        assertNull(resolveBackendImageUrl(null, "https://backend.com/api/"))
        assertNull(resolveBackendImageUrl("", "https://backend.com/api/"))
        assertNull(resolveBackendImageUrl("   ", "https://backend.com/api/"))
    }
}
