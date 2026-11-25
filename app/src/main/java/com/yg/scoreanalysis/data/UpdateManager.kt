package com.yg.scoreanalysis.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 更新流程的状态持有者：启动时静默检查、设置页手动检查、下载与安装。
 */
class UpdateManager(private val context: Context) {

    private val checker = UpdateChecker(context.applicationContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _checking = MutableStateFlow(false)
    val checking: StateFlow<Boolean> = _checking.asStateFlow()

    /** 用户点过「稍后」的版本，本次运行内不再自动弹窗 */
    private var skippedTag: String? = null

    private var job: Job? = null

    val current: Version get() = AppVersion.VERSION

    /** 启动时调用：发现新版本才进入 Ready，其余情况静默 */
    fun checkOnLaunch() {
        if (skippedTag != null) return
        if (job?.isActive == true) return
        job = scope.launch {
            _checking.value = true
            val result = checker.fetchLatest()
            _checking.value = false
            result.onSuccess { info ->
                if (info.version > current && info.tag != skippedTag) {
                    _state.value = UpdateState.Ready(info, current)
                }
            }.onFailure {
                // 启动检查失败保持静默，不打扰使用
            }
        }
    }

    /** 设置页手动检查：无论结果都给反馈 */
    fun checkManually() {
        if (job?.isActive == true) return
        skippedTag = null
        job = scope.launch {
            _checking.value = true
            _state.value = UpdateState.Checking
            val result = checker.fetchLatest()
            _checking.value = false
            _state.value = result.fold(
                onSuccess = { info ->
                    if (info.version > current) UpdateState.Ready(info, current)
                    else UpdateState.UpToDate(current)
                },
                onFailure = { UpdateState.Failed(it.message ?: "无法连接更新服务器") },
            )
        }
    }

    /** 下载并尝试安装；已下载完成时直接触发安装 */
    fun download() {
        val ready = _state.value as? UpdateState.Ready ?: return
        val done = ready.phase as? DownloadPhase.Done
        if (done != null) {
            checker.install(done.file)
            return
        }
        if (job?.isActive == true) return
        job = scope.launch {
            _state.value = ready.copy(phase = DownloadPhase.Running(0))
            val result = checker.download(ready.info) { percent ->
                val cur = _state.value as? UpdateState.Ready ?: return@download
                _state.value = cur.copy(phase = DownloadPhase.Running(percent))
            }
            val cur = _state.value as? UpdateState.Ready ?: return@launch
            result.onSuccess { file ->
                _state.value = cur.copy(phase = DownloadPhase.Done(file))
                withContext(Dispatchers.Main) { checker.install(file) }
            }.onFailure { t ->
                _state.value = cur.copy(phase = DownloadPhase.Failed(t.message ?: "下载失败"))
            }
        }
    }

    fun openReleasePage() {
        val ready = _state.value as? UpdateState.Ready ?: return
        checker.openUrl(ready.info.pageUrl)
    }

    /** 关闭更新弹窗，本版本本次运行不再自动提示 */
    fun dismiss() {
        skippedTag = (state.value as? UpdateState.Ready)?.info?.tag
        _state.value = UpdateState.Idle
    }

    /** 清掉「已是最新 / 失败」这类一次性反馈 */
    fun clearResult() {
        when (_state.value) {
            is UpdateState.UpToDate, is UpdateState.Failed -> _state.value = UpdateState.Idle
            else -> Unit
        }
    }
}
