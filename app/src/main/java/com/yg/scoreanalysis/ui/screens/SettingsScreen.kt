package com.yg.scoreanalysis.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yg.scoreanalysis.data.AppData
import com.yg.scoreanalysis.data.AppVersion
import com.yg.scoreanalysis.data.UpdateManager
import com.yg.scoreanalysis.data.UpdateState
import com.yg.scoreanalysis.ui.components.RoundedCard

@Composable
fun SettingsScreen(
    data: AppData,
    onBack: () -> Unit,
    onDarkMode: (Int) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClearAll: () -> Unit,
    embedded: Boolean = false,
    updateManager: UpdateManager? = null,
) {
    var confirmClear by remember { mutableStateOf(false) }
    val updateState by (updateManager?.state ?: kotlinx.coroutines.flow.MutableStateFlow<UpdateState>(UpdateState.Idle))
        .collectAsState()
    val checking by (updateManager?.checking ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!embedded) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "返回") }
            }
            Column {
                Text("设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "外观、数据与备份",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                RoundedCard(modifier = Modifier.fillMaxWidth(), corner = 24) {
                    Text("外观", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    val options = listOf("跟随系统", "浅色", "深色")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        options.forEachIndexed { i, label ->
                            SegmentedButton(
                                selected = data.settings.darkMode == i,
                                onClick = { onDarkMode(i) },
                                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                            ) { Text(label, style = MaterialTheme.typography.labelLarge) }
                        }
                    }
                }
            }
            item {
                RoundedCard(modifier = Modifier.fillMaxWidth(), corner = 24) {
                    Text("数据", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        data.subjects.size.toString() + " 门科目 · " + data.exams.size + " 场考试 · " + data.groups.size + " 次组考",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onExport,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            border = null,
                        ) { Text("导出 JSON", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) }
                        Button(
                            onClick = onImport,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            border = null,
                        ) { Text("导入 JSON", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) }
                    }
                }
            }
            item {
                RoundedCard(
                    modifier = Modifier.fillMaxWidth(),
                    corner = 24,
                    container = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                ) {
                    Text("清空全部数据", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "所有科目、考试与考试组都会被删除，且无法恢复。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { confirmClear = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) { Text("清空") }
                }
            }
            item {
                RoundedCard(modifier = Modifier.fillMaxWidth(), corner = 24) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("版本", style = MaterialTheme.typography.titleMedium)
                            Text(
                                AppVersion.VERSION.display(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Button(
                            onClick = { updateManager?.checkManually() },
                            enabled = updateManager != null && !checking,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        ) {
                            Text(
                                if (checking) "检查中…" else "检查更新",
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            )
                        }
                    }
                    when (val s = updateState) {
                        is UpdateState.UpToDate -> {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "已是最新版本",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        is UpdateState.Failed -> {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "检查失败：${s.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        is UpdateState.Ready -> {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "发现新版本 ${s.info.version.display()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        else -> Unit
                    }
                }
            }
            item {
                RoundedCard(modifier = Modifier.fillMaxWidth(), corner = 20) {
                    Text("关于", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "成绩分析 · 本地离线记录，数据只存在这台设备上。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("确定清空？") },
            text = { Text("这会删除设备上的全部成绩数据。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearAll()
                }) { Text("清空") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } },
        )
    }
}
