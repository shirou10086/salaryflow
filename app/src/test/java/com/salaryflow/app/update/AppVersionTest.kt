package com.salaryflow.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {

    @Test
    fun parsesTagsAndDebugSuffixes() {
        assertEquals("0.2.0", AppVersion.parse("v0.2.0")?.toString())
        assertEquals("0.2.0", AppVersion.parse("0.2.0-debug")?.toString())
        assertEquals("1", AppVersion.parse("1")?.toString())
        assertNull(AppVersion.parse("latest"))
    }

    @Test
    fun comparesNumericSegments() {
        assertTrue(AppVersion.parse("0.2.0")!! > AppVersion.parse("0.1.9")!!)
        assertTrue(AppVersion.parse("0.2.1")!! > AppVersion.parse("0.2")!!)
        assertTrue(AppVersion.parse("1.0.0")!! > AppVersion.parse("0.99.99")!!)
        assertEquals(AppVersion.parse("0.2"), AppVersion.parse("0.2.0"))
    }
}
