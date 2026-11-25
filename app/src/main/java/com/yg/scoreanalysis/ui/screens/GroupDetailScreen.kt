package com.yg.scoreanalysis.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yg.scoreanalysis.data.Aggregates
import com.yg.scoreanalysis.data.ExamGroup
import com.yg.scoreanalysis.data.Normalizer
import com.yg.scoreanalysis.data.totalScore
import com.yg.scoreanalysis.ui.chart.LineChart
import com.yg.scoreanalysis.ui.chart.MultiLineChart
import com.yg.scoreanalysis.ui.chart.Series
import com.yg.scoreanalysis.ui.components.Dot
import com.yg.scoreanalysis.ui.components.AdaptiveKpiRow
import com.yg.scoreanalysis.ui.components.Kpi
import com.yg.scoreanalysis.ui.components.Pill
import com.yg.scoreanalysis.ui.components.RoundedCard
import com.yg.scoreanalysis.ui.components.ScaleToggle
import com.yg.scoreanalysis.ui.theme.subjectColor

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GroupDetailScreen(
    groups: List<ExamGroup>,
    current: ExamGroup,
    subjectColorSeed: (String) -> Int,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    // 临时换算开关：不持久化，离开即恢复
    var scaled by remember(current.id) { mutableStateOf(false) }
    val chartSel = remember(current.id) { com.yg.scoreanalysis.ui.chart.ChartSelection() }
    var confirmDelete by remember { mutableStateOf(false) }
    val cfg = androidx.compose.ui.platform.LocalConfiguration.current
    val chartH = com.yg.scoreanalysis.ui.chart.chartHeightFor(
        cfg.screenWidthDp.dp, cfg.screenHeightDp.dp, 196.dp,
    )

    val ordered = groups.sortedBy { it.date }
    val labels = ordered.map { shortLabel(it.name) }
    val needsScale = Aggregates.groupsNeedNormalize(ordered)

    val totals = Normalizer.groupTotals(ordered, scaled)
    val classRankSeries = ordered.map { (it.classRank ?: it.totalRank)?.toDouble() }
    val gradeRankSeries = ordered.map { it.gradeRank?.toDouble() }
    val maxFull = ordered.maxOfOrNull { it.totalFullScore } ?: 0.0

    // 各科排名：为每个出现过的科目建一条序列
    val subjectIds = ordered.flatMap { g -> g.entries.map { it.subjectId } }.distinct()
    val multiSeries = subjectIds.map { sid ->
        val name = ordered.firstNotNullOfOrNull { g -> g.entries.firstOrNull { it.subjectId == sid }?.subjectName } ?: "科目"
        Series(
            name = name,
            color = subjectColor(subjectColorSeed(sid)),
            values = ordered.map { g -> g.entries.firstOrNull { it.subjectId == sid }?.rank?.toDouble() },
        )
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "返回") }
                    Column(Modifier.weight(1f)) {
                        Text(current.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            current.entries.size.toString() + " 科 · 总分满分 " + trimNum(current.totalFullScore),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "编辑") }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "删除")
                    }
                }
            }

            item {
                AdaptiveKpiRow(
                    items = listOf(
                        Kpi("本次总分", trimNum(current.totalScore), null, MaterialTheme.colorScheme.secondary),
                        Kpi("班级排名", (current.classRank ?: current.totalRank)?.let { "第 " + it } ?: "—", null, MaterialTheme.colorScheme.primary),
                        Kpi("组考次数", ordered.size.toString(), null, MaterialTheme.colorScheme.onSurfaceVariant),
                    ),
                )
            }

            if (needsScale) {
                item {
                    RoundedCard(modifier = Modifier.fillMaxWidth(), corner = 20) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("各次组考总分满分不同", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "换算后统一按百分制显示，退出本页自动恢复原始数据。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            ScaleToggle(checked = scaled, onCheckedChange = { scaled = it })
                        }
                    }
                }
            }

            item {
                RoundedCard(modifier = Modifier.fillMaxWidth(), corner = 24) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("总分趋势", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (scaled) Pill("百分制", MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(Modifier.height(6.dp))
                    AnimatedContent(
                        targetState = scaled,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "groupTotal",
                    ) { useScaled ->
                        LineChart(
                            values = Normalizer.groupTotals(ordered, useScaled),
                            labels = labels,
                            selection = chartSel,
                            chartId = "group-total",
                            color = MaterialTheme.colorScheme.secondary,
                            fixedMax = if (useScaled) 100.0 else maxFull.takeIf { it > 0 },
                            chartHeight = chartH,
                        )
                    }
                }
            }

            item {
                RoundedCard(modifier = Modifier.fillMaxWidth(), corner = 24) {
                    Text("总分班级排名趋势", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "纵轴越靠上名次越好",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    LineChart(
                        values = classRankSeries,
                        labels = labels,
                        selection = chartSel,
                        chartId = "group-class",
                        color = MaterialTheme.colorScheme.primary,
                        reverseY = true,
                        chartHeight = chartH,
                    )
                }
            }

            item {
                RoundedCard(modifier = Modifier.fillMaxWidth(), corner = 24) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("总分年级排名趋势", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Pill("年级", MaterialTheme.colorScheme.secondary)
                    }
                    Text(
                        "与班级排名分开统计，纵轴越靠上名次越好",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    LineChart(
                        values = gradeRankSeries,
                        labels = labels,
                        selection = chartSel,
                        chartId = "group-grade",
                        color = MaterialTheme.colorScheme.secondary,
                        reverseY = true,
                        chartHeight = chartH,
                    )
                }
            }

            item {
                RoundedCard(modifier = Modifier.fillMaxWidth(), corner = 24) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("各科班级排名趋势", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Pill("班级", MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        "各科排名按班级排名统计，点击图例可以只看某一科",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    var hidden by remember(current.id) { mutableStateOf(emptySet<String>()) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        multiSeries.forEach { s ->
                            val isHidden = s.name in hidden
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        hidden = if (isHidden) hidden - s.name else hidden + s.name
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Dot(if (isHidden) s.color.copy(alpha = 0.25f) else s.color, 8)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    s.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isHidden) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    MultiLineChart(
                        series = multiSeries,
                        labels = labels,
                        hiddenNames = hidden,
                        chartHeight = chartH + 14.dp,
                    )
                }
            }

            item {
                Text("本次各科成绩", style = MaterialTheme.typography.titleMedium)
            }
            items(current.entries.size) { i ->
                val e = current.entries[i]
                RoundedCard(modifier = Modifier.fillMaxWidth(), corner = 18) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(subjectColor(subjectColorSeed(e.subjectId)), 9)
                        Spacer(Modifier.width(8.dp))
                        Text(e.subjectName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(
                            trimNum(e.score) + " / " + trimNum(e.fullScore),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        buildString {
                            append(if (e.score / (if (e.fullScore > 0) e.fullScore else 1.0) >= 0.85) "发挥不错" else "继续加油")
                            e.rank?.let { append(" · 第 "); append(it); append(" 名") }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("删除这个考试组？") },
            text = { Text("「" + current.name + "」及其各科记录都会被移除，无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}
