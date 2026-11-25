package com.yg.scoreanalysis.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * 更新检查：读取 GitHub release，下载 apk 时优先走国内镜像。
 * 只用 HttpURLConnection，不引入额外网络库；所有请求都在 IO 线程。
 */
class UpdateChecker(private val context: Context) {

    private class HttpStatusException(val code: Int) : IOException("服务器返回 HTTP " + code)

    // ---------------- 拉取最新 release ----------------

    suspend fun fetchLatest(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        var last: Throwable? = null
        var reachable = false

        for (url in apiCandidates()) {
            try {
                val body = httpGet(url)
                reachable = true
                val info = ReleaseParser.parse(body)
                if (info != null) return@withContext Result.success(info)
                last = IOException("响应中没有可识别的版本号")
            } catch (t: HttpStatusException) {
                last = t
                // 仓库还没有发布 release，或已被限流：继续试镜像没有意义
                if (t.code == 404 || t.code == 403 || t.code == 401) break
            } catch (t: Throwable) {
                last = t
            }
        }

        val message = if (last is HttpStatusException && !reachable) {
            last.message ?: "服务器返回错误"
        } else {
            last?.message ?: "无法连接更新服务器"
        }
        Result.failure(IOException(message))
    }

    private fun apiCandidates(): List<String> {
        val api = GitHubRepo.API_LATEST
        return buildList {
            add(api)
            GitHubRepo.MIRRORS.forEach { add(it + api) }
        }
    }

    // ---------------- 下载 ----------------

    /**
     * 下载 release 里的 apk。优先镜像，最后回退官方地址。
     * onProgress 回调百分比（0-100；服务器未给长度时为 -1）。
     */
    suspend fun download(info: UpdateInfo, onProgress: (Int) -> Unit): Result<File> =
        withContext(Dispatchers.IO) {
            val remote = info.apkUrl
                ?: return@withContext Result.failure(IOException("该版本没有提供 APK 文件"))

            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val target = File(dir, "ScoreAnalysis-" + info.version.display() + ".apk")
            if (target.exists() && target.length() > MIN_APK_BYTES && isZip(target)) {
                onProgress(100)
                return@withContext Result.success(target)
            }

            val part = File(dir, "download.part")
            var last: Throwable? = null
            for (url in downloadCandidates(remote)) {
                try {
                    if (part.exists()) part.delete()
                    downloadTo(url, part, onProgress)
                    if (part.length() <= MIN_APK_BYTES) throw IOException("文件过小，可能不是 APK")
                    if (!isZip(part)) throw IOException("文件校验失败，可能不是 APK")
                    if (target.exists()) target.delete()
                    if (!part.renameTo(target)) {
                        part.copyTo(target, overwrite = true)
                        part.delete()
                    }
                    onProgress(100)
                    return@withContext Result.success(target)
                } catch (t: Throwable) {
                    last = t
                    if (part.exists()) part.delete()
                }
            }
            Result.failure(last ?: IOException("下载失败"))
        }

    /** 先试镜像前缀，最后才直连 github */
    private fun downloadCandidates(remote: String): List<String> = buildList {
        GitHubRepo.MIRRORS.forEach { add(it + remote) }
        add(remote)
    }

    private fun downloadTo(url: String, target: File, onProgress: (Int) -> Unit) {
        val conn = openConnection(url)
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP " + code)
            val total = conn.contentLengthLong
            var read = 0L
            var lastPercent = -2
            conn.inputStream.use { input ->
                target.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        out.write(buf, 0, n)
                        read += n
                        val percent = if (total > 0) ((read * 100) / total).toInt() else -1
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress(percent)
                        }
                    }
                }
            }
            if (total > 0 && read < total) throw IOException("下载不完整")
        } finally {
            conn.disconnect()
        }
    }

    // ---------------- 安装 / 打开页面 ----------------

    fun install(file: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    // ---------------- 网络工具 ----------------

    private fun httpGet(url: String): String {
        val conn = openConnection(url, accept = "application/vnd.github+json")
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw HttpStatusException(code)
            return conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        } finally {
            conn.disconnect()
        }
    }

    private fun openConnection(url: String, accept: String? = null): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 12_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "ScoreAnalysis/" + AppVersion.NAME + " (Android)")
            if (accept != null) setRequestProperty("Accept", accept)
        }

    private fun isZip(file: File): Boolean = runCatching {
        file.inputStream().use { input ->
            val head = ByteArray(4)
            if (input.read(head) < 4) return@use false
            head[0] == 0x50.toByte() && head[1] == 0x4B.toByte() &&
                head[2] == 0x03.toByte() && head[3] == 0x04.toByte()
        }
    }.getOrDefault(false)

    companion object {
        /** 低于这个大小的不可能是装着应用的 apk */
        private const val MIN_APK_BYTES = 200_000L
    }
}
