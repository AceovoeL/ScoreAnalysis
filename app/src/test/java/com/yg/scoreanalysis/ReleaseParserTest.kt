package com.yg.scoreanalysis

import com.yg.scoreanalysis.data.ReleaseParser
import com.yg.scoreanalysis.data.Version
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReleaseParserTest {

    @Test
    fun parsesTagBodyAndApk() {
        val body = """
            {
              "tag_name": "v1.0.1",
              "name": "release 1.0.1",
              "body": "修复若干问题",
              "html_url": "https://github.com/AceovoeL/ScoreAnalysis/releases/tag/v1.0.1",
              "assets": [
                {
                  "name": "app-release.apk",
                  "browser_download_url": "https://github.com/AceovoeL/ScoreAnalysis/releases/download/v1.0.1/app-release.apk",
                  "size": 12345678
                }
              ]
            }
        """.trimIndent()
        val info = ReleaseParser.parse(body)!!
        assertEquals(Version(1, 0, 1), info.version)
        assertEquals("修复若干问题", info.notes)
        assertEquals("app-release.apk", info.apkName)
        assertEquals(12345678L, info.apkSize)
        assertTrue(info.apkUrl!!.endsWith("v1.0.1/app-release.apk"))
    }

    @Test
    fun prefersNonDebugApk() {
        val body = """
            {
              "tag_name": "v1.1.0",
              "body": "x",
              "assets": [
                {"name": "app-debug.apk", "browser_download_url": "https://e/d.apk", "size": 1},
                {"name": "app-release.apk", "browser_download_url": "https://e/r.apk", "size": 2}
              ]
            }
        """.trimIndent()
        assertEquals("app-release.apk", ReleaseParser.parse(body)!!.apkName)
    }

    @Test
    fun toleratesMissingApk() {
        val body = """
            {"tag_name": "v1.0.2", "body": "说明", "assets": []}
        """.trimIndent()
        val info = ReleaseParser.parse(body)!!
        assertNull(info.apkUrl)
        assertEquals("v1.0.2", info.tag)
    }

    @Test
    fun rejectsUnparsableTag() {
        assertNull(ReleaseParser.parse("""{"tag_name":"nightly","assets":[]}"""))
    }

    @Test
    fun rejectsBrokenJson() {
        assertNull(ReleaseParser.parse("not json at all"))
        assertNull(ReleaseParser.parse(""))
        assertNull(ReleaseParser.parse("""{"assets":[]}"""))
    }
}
