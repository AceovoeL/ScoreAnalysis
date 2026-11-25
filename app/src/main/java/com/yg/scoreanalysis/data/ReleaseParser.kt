package com.yg.scoreanalysis.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * GitHub release JSON 的纯解析逻辑，与网络无关，便于单元测试。
 */
object ReleaseParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** 返回 null 表示该响应不是可用的 release（字段缺失、tag 无法解析等）。 */
    fun parse(body: String): UpdateInfo? = runCatching {
        val root = json.parseToJsonElement(body).jsonObject
        val tag = root["tag_name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: root["name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: return null
        val version = Version.parse(tag) ?: return null

        val assets = root["assets"]?.jsonArray ?: return@runCatching null
        val apks = assets.mapNotNull { el ->
            val o = el.jsonObject
            val name = o["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val url = o["browser_download_url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            if (!name.endsWith(".apk", ignoreCase = true)) return@mapNotNull null
            ApkAsset(name, url, o["size"]?.jsonPrimitive?.longOrNull ?: 0L)
        }
        // 优先非 debug 产物；没有 apk 也照常返回（只提示、不下载）
        val picked = apks.firstOrNull { !it.name.contains("debug", ignoreCase = true) } ?: apks.firstOrNull()
        val tagSuffix = "/tag/" + tag

        UpdateInfo(
            version = version,
            tag = tag,
            notes = root["body"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty(),
            pageUrl = root["html_url"]?.jsonPrimitive?.contentOrNull
                ?: (GitHubRepo.RELEASES_PAGE + tagSuffix),
            apkUrl = picked?.url,
            apkName = picked?.name,
            apkSize = picked?.size ?: 0L,
        )
    }.getOrNull()
}

/** release 里的一个 apk 资产 */
data class ApkAsset(val name: String, val url: String, val size: Long)
