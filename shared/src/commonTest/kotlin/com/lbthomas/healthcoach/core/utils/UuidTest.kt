package com.lbthomas.healthcoach.core.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UuidTest {

    @Test
    fun testGenerateUuidFormatAndVersion7() {
        val uuid = generateUuid()
        assertEquals(36, uuid.length)

        // RFC 9562 UUID format: 8-4-4-4-12
        val uuidPattern = Regex("""^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$""")
        assertTrue(uuidPattern.matches(uuid), "Generated UUID '$uuid' should be a valid lowercase UUIDv7")
    }

    @Test
    fun testMonotonicityAndUniqueness() {
        val count = 100
        val uuids = (1..count).map { generateUuid() }

        // All should be unique
        assertEquals(count, uuids.toSet().size)

        // All should be sorted in ascending lexicographical order
        val sorted = uuids.sorted()
        assertEquals(sorted, uuids)
    }
}
