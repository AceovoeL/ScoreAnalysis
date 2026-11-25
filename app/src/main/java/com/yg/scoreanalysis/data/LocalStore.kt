package com.yg.scoreanalysis.data

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 原子写入：先写临时文件，再改名替换，并保留一份 .bak 兜底。
 */
class LocalStore(context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val dir: File = context.filesDir
    private val file = File(dir, "app_data.json")
    private val backup = File(dir, "app_data.json.bak")
    private val temp = File(dir, "app_data.json.tmp")

    /** 上次加载是否发生过损坏回退，用于提示一次 */
    var lastLoadRecovered: Boolean = false
        private set

    fun load(): AppData {
        lastLoadRecovered = false
        readFile(file)?.let { return migrate(it) }
        val fromBackup = readFile(backup)
        if (fromBackup != null) {
            lastLoadRecovered = true
            return migrate(fromBackup)
        }
        if (file.exists() || backup.exists()) lastLoadRecovered = true
        return AppData()
    }

    /** 旧数据兼容：历史版本默认 darkMode=0（跟随系统），新版本默认深色 */
    private fun migrate(d: AppData): AppData =
        if (d.version == 1 && d.settings.darkMode == 0) d.copy(settings = d.settings.copy(darkMode = 2)) else d

    private fun readFile(f: File): AppData? {
        if (!f.exists() || f.length() == 0L) return null
        return try {
            json.decodeFromString(AppData.serializer(), f.readText(Charsets.UTF_8))
        } catch (t: Throwable) {
            null
        }
    }

    fun save(data: AppData) {
        try {
            val text = json.encodeToString(AppData.serializer(), data)
            temp.writeText(text, Charsets.UTF_8)
            if (file.exists()) {
                try {
                    file.copyTo(backup, overwrite = true)
                } catch (_: Throwable) {
                }
            }
            if (!temp.renameTo(file)) {
                file.writeText(text, Charsets.UTF_8)
                temp.delete()
            }
        } catch (_: Throwable) {
        }
    }

    fun clear() {
        file.delete()
        backup.delete()
        temp.delete()
    }
}
