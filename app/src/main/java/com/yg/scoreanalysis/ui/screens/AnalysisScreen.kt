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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.yg.scoreanalysis.data.AppData
import com.yg.scoreanalysis.data.Normalizer
import com.yg.scoreanalysis.data.totalScore
import com.yg.scoreanalysis.ui.chart.LineChart
import com.yg.scoreanalysis.ui.components.BigValue
import com.yg.scoreanalysis.ui.components.CardHeader
import com.yg.scoreanalysis.ui.components.SectionCard
import com.yg.scoreanalysis.ui.components.Tag
import com.yg.scoreanalysis.ui.theme.subjectColor

/** 分析：按科目切换，看单科趋势与分布；以及组考排名对比 */
@Composable
fun AnalysisScreen(data: AppData, onOpenSubject: (String) -> Unit) {
    val chartSel = remember { com.yg.scoreanalysis.ui.chart.ChartSelection() }
    var selectedId by remember { mutableStateOf(data.subjects.firstOrNull()?.id) }
    val subject = data.subjects.firstOrNull { it.id == selectedId }
    val exams = subject?.let { Aggregates.examsOf(data, it.id) } ?: emptyList()
    val color = subjectColor(subject?.colorSeed ?: 0)
    val groups = Aggregates.groupsOf(data)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("分析", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "单科趋势与组考排名",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (data.subjects.isEmpty()) {
            item {
                SectionCard(Modifier.fillMaxWidth()) {
                    Text(
                        "添加科目后才能分析单科趋势。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(data.subjects.size) { i ->
                        val s = data.subjects[i]
                        val sel = s.id == selectedId
                        FilterChip(
                            selected = sel,
                            onClick = { selectedId = s.id },
                            label = { Text(s.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = subjectColor(s.colorSeed).copy(alpha = 0.22f),
                                selectedLabelColor = subjectColor(s.colorSeed),
                            ),
                        )
                    }
                }
            }

            if (subject != null) {
                item {
                    SectionCard(Modifier.fillMaxWidth(), corner = 24) {
                        CardHeader(
                            subject.name + " · 共 " + exams.size + " 次",
                            trailing = { Tag("满分 " + fmtNum(subject.fullScore), color = color) },
                        )
                        Spacer(Modifier.height(8.dp))
                        val best = Aggregates.bestExam(exams)
                        BigValue(
                            value = best?.let { fmtNum(it.score) } ?: "—",
                            unit = "/ " + fmtNum(subject.fullScore),
                            suffix = "最高",
                            color = color,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "平均 " + fmtNum(Aggregates.averageScore(exams)) +
                                (best?.name?.let { "  ·  最高分来自 " + it } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item {
                    SectionCard(Modifier.fillMaxWidth()) {
                        CardHeader("单科趋势")
                        Spacer(Modifier.height(4.dp))
                        LineChart(
                            values = Normalizer.examScores(exams, subject.fullScore, false),
                            labels = exams.map { shortName(it.name) },
                            selection = chartSel,
                            chartId = "analysis-score",
                            color = color,
                            fixedMax = subject.fullScore.takeIf { it > 0 },
                        )
                    }
                }

                item {
                    SectionCard(Modifier.fillMaxWidth()) {
                        CardHeader("单科排名趋势", trailing = { Tag("班级", color = MaterialTheme.colorScheme.primary) })
                        Spacer(Modifier.height(4.dp))
                        LineChart(
                            values = Aggregates.rankSeries(exams),
                            labels = exams.map { shortName(it.name) },
                            selection = chartSel,
                            chartId = "analysis-rank",
                            color = MaterialTheme.colorScheme.primary,
                            reverseY = true,
                        )
                    }
                }
            }
        }

        if (groups.size >= 2) {
            item {
                SectionCard(Modifier.fillMaxWidth()) {
                    CardHeader("组考总分", trailing = { Tag(groups.size.toString() + " 次") })
                    Spacer(Modifier.height(4.dp))
                    LineChart(
                        values = groups.map { it.totalScore },
                        labels = groups.map { shortName(it.name) },
                        selection = chartSel,
                        chartId = "analysis-total",
                        color = MaterialTheme.colorScheme.secondary,
                        fixedMax = groups.maxOf { it.totalFullScore }.takeIf { it > 0 },
                    )
                }
            }
            item {
                SectionCard(Modifier.fillMaxWidth()) {
                    CardHeader("班级排名", trailing = { Tag("总分", color = MaterialTheme.colorScheme.primary) })
                    Spacer(Modifier.height(4.dp))
                    LineChart(
                        values = groups.map { (it.classRank ?: it.totalRank)?.toDouble() },
                        labels = groups.map { shortName(it.name) },
                        selection = chartSel,
                        chartId = "analysis-class",
                        color = MaterialTheme.colorScheme.primary,
                        reverseY = true,
                    )
                }
            }
            item {
                SectionCard(Modifier.fillMaxWidth()) {
                    CardHeader("年级排名", trailing = { Tag("总分", color = MaterialTheme.colorScheme.secondary) })
                    Spacer(Modifier.height(4.dp))
                    LineChart(
                        values = groups.map { it.gradeRank?.toDouble() },
                        labels = groups.map { shortName(it.name) },
                        selection = chartSel,
                        chartId = "analysis-grade",
                        color = MaterialTheme.colorScheme.secondary,
                        reverseY = true,
                    )
                }
            }
        }
    }
}
