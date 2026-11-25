package com.yg.scoreanalysis

import com.yg.scoreanalysis.data.Version
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VersionTest {

    @Test
    fun parseWithPrefix() {
        assertEquals(Version(1, 0, 0), Version.parse("v1.0.0"))
        assertEquals(Version(1, 2, 3), Version.parse("v1.2.3"))
    }

    @Test
    fun parseWithoutPrefix() {
        assertEquals(Version(2, 3, 4), Version.parse("2.3.4"))
    }

    @Test
    fun parsePartial() {
        assertEquals(Version(1, 2, 0), Version.parse("v1.2"))
        assertEquals(Version(3, 0, 0), Version.parse("3"))
    }

    @Test
    fun parseIgnoresSuffix() {
        assertEquals(Version(1, 0, 1), Version.parse("v1.0.1-beta.2"))
        assertEquals(Version(1, 1, 0), Version.parse("v1.1.0+build.5"))
    }

    @Test
    fun parseRejectsGarbage() {
        assertNull(Version.parse(null))
        assertNull(Version.parse(""))
        assertNull(Version.parse("   "))
        assertNull(Version.parse("latest"))
        assertNull(Version.parse("v"))
        assertNull(Version.parse("v-next"))
    }

    @Test
    fun orderingFollowsSemanticRules() {
        assertTrue(Version.parse("v1.0.1")!! > Version.parse("v1.0.0")!!)
        assertTrue(Version.parse("v1.1.0")!! > Version.parse("v1.0.9")!!)
        assertTrue(Version.parse("v2.0.0")!! > Version.parse("v1.9.9")!!)
        assertTrue(Version.parse("v1.0.10")!! > Version.parse("v1.0.9")!!)
    }

    @Test
    fun codeIsMonotonic() {
        val seq = listOf("v1.0.0", "v1.0.1", "v1.0.9", "v1.1.0", "v1.1.1", "v2.0.0")
        val codes = seq.map { Version.parse(it)!!.code() }
        codes.zipWithNext().forEach { (a, b) -> assertTrue(b > a, "code 必须递增: $a -> $b") }
    }

    @Test
    fun displayRoundTrip() {
        assertEquals("v1.0.0", Version(1, 0, 0).display())
        assertEquals("v1.10.2", Version(1, 10, 2).display())
    }

    @Test
    fun currentVersionMatchesGradle() {
        // 防止 build.gradle.kts 与 AppVersion 脱节
        assertEquals(Version(1, 0, 0), com.yg.scoreanalysis.data.AppVersion.VERSION)
    }
}
