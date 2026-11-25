package com.yg.scoreanalysis.data

/** 当前应用版本；每次发版只改这里和 app/build.gradle.kts */
object AppVersion {
    const val NAME = "1.0.0"

    /** major * 10000 + minor * 100 + patch，保证单调递增 */
    const val CODE = 10000

    val VERSION = Version(1, 0, 0)
}

/** 语义化版本：主.次.修订 */
data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version> {

    override fun compareTo(other: Version): Int {
        if (major != other.major) return major - other.major
        if (minor != other.minor) return minor - other.minor
        return patch - other.patch
    }

    fun display(): String = "v$major.$minor.$patch"

    fun code(): Int = major * 10_000 + minor * 100 + patch

    companion object {
        /**
         * 解析 tag：支持 "v1.0.0"、"1.0.0"、"v1.2"、"1.0.0-beta.1"。
         * 解析失败返回 null（调用方应忽略该版本，避免把奇怪的 tag 当成新版）。
         */
        fun parse(raw: String?): Version? {
            if (raw.isNullOrBlank()) return null
            var s = raw.trim()
            if (s.startsWith("v", ignoreCase = true)) s = s.substring(1)
            s = s.substringBefore('-').substringBefore('+').trim()
            if (s.isEmpty()) return null
            val parts = s.split('.')
            if (parts.isEmpty()) return null
            fun num(i: Int): Int? = parts.getOrNull(i)?.trim()?.toIntOrNull()
            val major = num(0) ?: return null
            val minor = num(1) ?: 0
            val patch = num(2) ?: 0
            if (major < 0 || minor < 0 || patch < 0) return null
            return Version(major, minor, patch)
        }
    }
}

/** 一个 release 的解析结果 */
data class UpdateInfo(
    val version: Version,
    val tag: String,
    /** release 正文，用作更新说明 */
    val notes: String,
    /** release 页面地址（浏览器打开） */
    val pageUrl: String,
    /** release 里 apk 资产的 github 原始下载地址 */
    val apkUrl: String?,
    val apkName: String?,
    val apkSize: Long,
)
