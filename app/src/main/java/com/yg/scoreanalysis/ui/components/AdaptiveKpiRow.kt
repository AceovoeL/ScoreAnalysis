package com.yg.scoreanalysis.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class Kpi(val label: String, val value: String, val hint: String? = null, val accent: Color? = null)

/**
 * 自适应 KPI 行：
 * - 宽屏（≥360dp）三项并排
 * - 窄屏两项一行，第三项独占一行
 */
@Composable
fun AdaptiveKpiRow(
    items: List<Kpi>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val narrow = maxWidth < 360.dp
        if (!narrow || items.size <= 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { item ->
                    KpiTile(
                        label = item.label,
                        value = item.value,
                        modifier = Modifier.weight(1f),
                        hint = item.hint,
                        accent = item.accent ?: androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.chunked(2).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowItems.forEach { item ->
                            KpiTile(
                                label = item.label,
                                value = item.value,
                                modifier = Modifier.weight(1f),
                                hint = item.hint,
                                accent = item.accent ?: androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            )
                        }
                        // 补齐空位，避免最后一个单独占满整行时被拉伸
                        if (rowItems.size == 1 && items.size > 1) {
                            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
