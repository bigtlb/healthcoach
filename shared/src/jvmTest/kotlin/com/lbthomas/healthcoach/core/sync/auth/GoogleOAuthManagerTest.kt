package com.lbthomas.healthcoach.core.sync.auth

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GoogleOAuthManagerTest {

    @Test
    fun testGetResolvedClientIdFromLocalProperties() {
        val resolved = GoogleOAuthManager.getResolvedClientId()
        assertNotNull(resolved)
        assertTrue(resolved.isNotBlank())
        // Should match the desktop client ID set in local.properties or default
        assertTrue(resolved.contains("apps.googleusercontent.com"))
    }

    @Test
    fun testGetResolvedClientSecret() {
        val secret = GoogleOAuthManager.getResolvedClientSecret()
        assertNotNull(secret)
        assertTrue(secret.isNotBlank())
        assertTrue(secret.startsWith("GOCSPX-"))
    }
}
