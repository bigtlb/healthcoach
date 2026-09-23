package com.lbthomas.healthcoach.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppInfoTest {

    @Test
    fun testAppInfoMetadata() {
        assertEquals("Health Coach", AppInfo.APP_NAME)
        assertEquals("0.9.0", AppInfo.APP_VERSION)
        assertEquals("Thomas Baker", AppInfo.AUTHOR)
        assertEquals("https://github.com/bigtlb/healthcoach", AppInfo.GITHUB_URL)
        assertEquals("Apache License 2.0", AppInfo.LICENSE_NAME)
        assertTrue(AppInfo.LICENSE_URL.startsWith("https://"))
    }

    @Test
    fun testAttributionsNotEmpty() {
        assertFalse(AppInfo.attributions.isEmpty())
        assertTrue(AppInfo.attributions.size >= 8)

        AppInfo.attributions.forEach { attribution ->
            assertTrue(attribution.name.isNotBlank(), "Attribution name should not be blank")
            assertTrue(attribution.copyright.isNotBlank(), "Attribution copyright should not be blank")
            assertTrue(attribution.licenseName.isNotBlank(), "Attribution licenseName should not be blank")
            assertTrue(attribution.licenseUrl.startsWith("https://"), "Attribution licenseUrl must be a valid URL")
            assertTrue(attribution.projectUrl.startsWith("https://"), "Attribution projectUrl must be a valid URL")
        }
    }

    @Test
    fun testApacheLicenseTextPresent() {
        assertTrue(AppInfo.APACHE_LICENSE_TEXT.contains("Apache License"))
        assertTrue(AppInfo.APACHE_LICENSE_TEXT.contains("Version 2.0"))
        assertTrue(AppInfo.APACHE_LICENSE_TEXT.contains("Thomas Baker"))
    }
}
