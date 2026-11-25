package com.yg.scoreanalysis.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.yg.scoreanalysis.data.Exam
import com.yg.scoreanalysis.data.Normalizer
import com.yg.scoreanalysis.data.Subject
import com.yg.scoreanalysis.ui.chart.LineChart
import com.yg.scoreanalysis.ui.components.AdaptiveKpiRow
import com.yg.scoreanalysis.ui.components.Kpi
import com.yg.scoreanalysis.ui.components.Pill
import com.yg.scoreanalysis.ui.components.RoundedCard
import com.yg.scoreanalysis.ui.components.ScaleToggle
import com.yg.scoreanalysis.ui.components.Dot
import com.yg.scoreanalysis.ui.theme.subjectColor

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SubjectDetailScreen(
    subject: Subject,
    exams: List<Exam>,
    onBack: () -> Unit,
    onAddExam: () -> Unit,
    onEditExam: (Exam) -> Unit,
    onDeleteExam: (Exam) -> Unit,
    onEditSubject: () -> Unit,
) {
    // 仅存于本次进入：退出页面即恢复原始视图
    var scaled by remember(subject.id) { mutableStateOf(false) }
    val chartSel = remember(subject.id) { com.yg.scoreanalysis.ui.chart.ChartSelection() }
    val cfg = androidx.compose.ui.platform.LocalConfiguration.current
    val chartH = com.yg.scoreanalysis.ui.chart.chartHeightFor(
        cfg.screenWidthDp.dp, cfg.screenHeightDp.dp, 196.dp,
    )
    var pendingDelete by remember { mutableStateOf<Exam?>(null) }

    val sorted = exams.sortedBy { it.date }
    val best = Aggregates.bestExam(sorted)
    val avg = Aggregates.averageScore(sorted)
    val needsScale = Aggregates.subjectNeedsNormalize(subject, sorted)
    val color = subjectColor(subject.colorSeed)

    val scoreSeries = Normalizer.examScores(sorted, subject.fullScore, scaled)
    val rankSeries = Aggregates.rankSeries(sorted)
    val labels = sorted.map { shortLabel(it.name) }

    val latestRank = sorted.lastOrNull { it.rank != null }?.rank

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
                    }
                    Column(Modifier.weight(1f)) {
                        Text(subject.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "满分 " + fmt(subject.fullScore) + " · " + sorted.size + " 场考试",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onEditSubject) {
                        Icon(Icons.Rounded.Edit, contentDescription = "编辑科目")
                    }
                }
            }

            item {
                AdaptiveKpiRow(
                    items = listOf(
                        Kpi("平均分", fmt(avg), null, color),
                        Kpi("最高分", best?.let { fmt(it.score) } ?: "—", best?.name, color),
                        Kpi("最近排名", latestRank?.let { "第 " + it } ?: "—", null, MaterialTheme.colorScheme.secondary),
                    ),
                )
            }

            if (needsScale) {
                item {
                    RoundedCard(modifier = Modifier.fillMaxWidth(), corner = 20) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("有些场次满分不同", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "开启换算后按得分率统一到科目满分查看，仅影响图表显示。",
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
                        Text("成绩趋势", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (scaled) Pill("换算后", MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(Modifier.height(6.dp))
                    AnimatedContent(
                        targetState = scaled,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "scoreTrend",
                    ) { useScaled ->
                        LineChart(
                            values = Normalizer.examScores(sorted, subject.fullScore, useScaled),
                            labels = labels,
                            selection = chartSel,
                            chartId = "subject-detail-score",
                            color = color,
                            // 纵轴顶端始终是科目满分，不因取整跳到 150
                            fixedMax = subject.fullScore.takeIf { it > 0 },
                            chartHeight = chartH,
                        )
                    }
                }
            }

            item {
                RoundedCard(modifier = Modifier.fillMaxWidth(), corner = 24) {
                    Text("排名趋势", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "纵轴越靠上名次越好",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    LineChart(
                        values = rankSeries,
                        labels = labels,
                        selection = chartSel,
                        chartId = "subject-detail-rank",
                        color = MaterialTheme.colorScheme.secondary,
                        reverseY = true,
                        chartHeight = chartH,
                    )
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("考试记录", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Pill("添加", MaterialTheme.colorScheme.primary, onClick = onAddExam)
                }
            }

            if (sorted.isEmpty()) {
                item {
                    RoundedCard(modifier = Modifier.fillMaxWidth(), corner = 22) {
                        Text(
                            "还没有添加考试。可以点上面的「添加」单独记一场，或在考试组里录入本场成绩。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(sorted.reversed(), key = { it.id }) { exam ->
                    val isBest = best != null && exam.id == best.id
                    val fromGroup = exam.fromGroup
                    RoundedCard(
                        modifier = Modifier.fillMaxWidth(),
                        corner = 22,
                        container = if (isBest) color.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                        onClick = { onEditExam(exam) },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isBest) {
                                Icon(
                                    Icons.Rounded.EmojiEvents,
                                    contentDescription = "最高分",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    exam.name.ifBlank { "未命名考试" },
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    buildString {
                                        append(fmt(exam.score))
                                        append(" / ")
                                        append(fmt(exam.examFullScore))
                                        exam.rank?.let { append(" · 第 "); append(it); append(" 名") }
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (fromGroup) {
                                Pill("考试组", MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(4.dp))
                            }
                            if (isBest) {
                                Pill("最高", MaterialTheme.colorScheme.secondary, filled = true)
                                Spacer(Modifier.width(4.dp))
                            }
                            if (fromGroup) {
                                // 来自考试组：不给删除按钮，点击进考试组里改
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = "去考试组",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            } else IconButton(onClick = { pendingDelete = exam }) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        androidx.compose.material3.ExtendedFloatingActionButton(
            onClick = onAddExam,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("记一场")
        }
    }

    pendingDelete?.let { exam ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            shape = RoundedCornerShape(24.dp),
            title = { Text("删除这场考试？") },
            text = { Text("「" + exam.name.ifBlank { "未命名考试" } + "」的记录将被移除，无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteExam(exam)
                    pendingDelete = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}
