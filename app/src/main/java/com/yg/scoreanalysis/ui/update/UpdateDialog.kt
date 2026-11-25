package com.yg.scoreanalysis.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.yg.scoreanalysis.data.DownloadPhase
import com.yg.scoreanalysis.data.UpdateState

/**
 * 有新版本时的弹窗：展示 release 正文作为更新说明，并提供下载 / 稍后。
 */
@Composable
fun UpdateDialog(
    state: UpdateState.Ready,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
    onOpenPage: () -> Unit,
) {
    val downloading = state.phase is DownloadPhase.Running
    AlertDialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !downloading, dismissOnClickOutside = !downloading),
        shape = RoundedCornerShape(26.dp),
        title = {
            Column {
                Text(
                    "发现新版本 ${state.info.version.display()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "当前版本 ${state.current.display()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column {
                Column(
                    Modifier
                        .heightIn(max = 340.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        if (state.info.notes.isBlank()) "该版本没有填写更新说明。" else state.info.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                when (val p = state.phase) {
                    is DownloadPhase.Running -> {
                        Spacer(Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { (p.percent.coerceAtLeast(0)) / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (p.percent < 0) "正在下载…" else "正在下载… ${p.percent}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is DownloadPhase.Done -> {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "下载完成，若没有自动弹出安装界面，点下方按钮继续。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is DownloadPhase.Failed -> {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "下载失败：${p.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    DownloadPhase.Idle -> Unit
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                when (val p = state.phase) {
                    is DownloadPhase.Done -> Button(
                        onClick = onDownload,
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("立即安装", fontWeight = FontWeight.SemiBold) }
                    else -> Button(
                        onClick = onDownload,
                        enabled = !downloading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) { Text(if (p is DownloadPhase.Failed) "重试" else "下载更新", fontWeight = FontWeight.SemiBold) }
                }
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onOpenPage, enabled = !downloading) { Text("查看页面") }
                TextButton(onClick = onDismiss, enabled = !downloading) { Text("稍后") }
            }
        },
    )
}
