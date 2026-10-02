package com.lbthomas.healthcoach.core.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class StringUtilsTest {

    @Test
    fun testToTitleCase() {
        assertEquals("", "".toTitleCase())
        assertEquals("   ", "   ".toTitleCase())
        assertEquals("John", "john".toTitleCase())
        assertEquals("John Doe", "john doe".toTitleCase())
        assertEquals("John Doe", "JOHN DOE".toTitleCase())
        assertEquals("Mary-Jane Watson", "mary-jane watson".toTitleCase())
        assertEquals("Mary-Jane Watson", "MARY-JANE WATSON".toTitleCase())
        assertEquals("Alice In Wonderland", "  alice   in   wonderland  ".toTitleCase())
    }
}
