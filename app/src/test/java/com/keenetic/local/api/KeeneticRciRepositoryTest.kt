package com.keenetic.local.api

import org.junit.Assert.*
import org.junit.Test

class KeeneticRciRepositoryTest {

    @Test
    fun testDefaultBaseUrl() {
        val repo = KeeneticRciRepository()
        assertEquals("192.168.1.1", repo.currentHost)
        assertEquals("80", repo.currentPort)
        assertFalse(repo.isHttps)
        assertEquals("http://192.168.1.1:80/", repo.currentBaseUrl)
    }

    @Test
    fun testConfigureBaseUrlCustomHostAndPort() {
        val repo = KeeneticRciRepository()
        repo.configureBaseUrl("10.0.0.1", "8080", useHttps = false)
        assertEquals("10.0.0.1", repo.currentHost)
        assertEquals("8080", repo.currentPort)
        assertFalse(repo.isHttps)
        assertEquals("http://10.0.0.1:8080/", repo.currentBaseUrl)
    }

    @Test
    fun testConfigureBaseUrlHttps() {
        val repo = KeeneticRciRepository()
        repo.configureBaseUrl("myrouter.keenetic.pro", "443", useHttps = true)
        assertEquals("myrouter.keenetic.pro", repo.currentHost)
        assertEquals("443", repo.currentPort)
        assertTrue(repo.isHttps)
        assertEquals("https://myrouter.keenetic.pro:443/", repo.currentBaseUrl)
    }

    @Test
    fun testSetBaseUrlFullString() {
        val repo = KeeneticRciRepository()
        repo.setBaseUrl("https://192.168.1.1:8443")
        assertEquals("192.168.1.1", repo.currentHost)
        assertEquals("8443", repo.currentPort)
        assertTrue(repo.isHttps)
        assertEquals("https://192.168.1.1:8443/", repo.currentBaseUrl)
    }

    @Test
    fun testAuthHeaderSupport() {
        val repo = KeeneticRciRepository()
        assertNull(repo.getAuthHeader())

        repo.setBearerToken("secret_token_123")
        assertEquals("Bearer secret_token_123", repo.getAuthHeader())

        repo.setAuthHeader("Token custom_key_abc")
        assertEquals("Token custom_key_abc", repo.getAuthHeader())

        repo.setBasicAuth("admin", "keenetic_pass")
        assertNotNull(repo.getAuthHeader())
        assertTrue(repo.getAuthHeader()!!.startsWith("Basic "))

        repo.clearAuth()
        assertNull(repo.getAuthHeader())
    }
}
