package com.yg.scoreanalysis.data

import java.io.File

/** 下载阶段 */
sealed interface DownloadPhase {
    data object Idle : DownloadPhase
    data class Running(val percent: Int) : DownloadPhase
    data class Done(val file: File) : DownloadPhase
    data class Failed(val message: String) : DownloadPhase
}

/** 更新检查 / 下载的整体状态 */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val current: Version) : UpdateState
    data class Failed(val message: String) : UpdateState
    data class Ready(
        val info: UpdateInfo,
        val current: Version,
        val phase: DownloadPhase = DownloadPhase.Idle,
    ) : UpdateState
}
