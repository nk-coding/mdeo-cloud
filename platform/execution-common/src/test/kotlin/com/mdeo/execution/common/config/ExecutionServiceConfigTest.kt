package com.mdeo.execution.common.config

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Deployments that give the services only the backend's origin: the URL has to come out as the
 * API base, or the keys and the data alike sit one 404 away.
 */
class ExecutionServiceConfigTest {
    @Test
    fun `completes a bare origin with the api path`() {
        assertEquals("http://backend:8080/api", normalizeBackendApiUrl("http://backend:8080"))
        assertEquals("http://backend:8080/api", normalizeBackendApiUrl("http://backend:8080/"))
    }

    @Test
    fun `leaves a url that already has a path`() {
        assertEquals("http://backend:8080/api", normalizeBackendApiUrl("http://backend:8080/api"))
        assertEquals("http://backend:8080/api", normalizeBackendApiUrl("http://backend:8080/api/"))
        assertEquals(
            "https://mdeo.example.org/platform/api",
            normalizeBackendApiUrl("https://mdeo.example.org/platform/api")
        )
    }

    @Test
    fun `leaves what is not a hierarchical url`() {
        assertEquals("not a url", normalizeBackendApiUrl("not a url"))
        assertEquals("backend:8080", normalizeBackendApiUrl("backend:8080"))
    }
}
