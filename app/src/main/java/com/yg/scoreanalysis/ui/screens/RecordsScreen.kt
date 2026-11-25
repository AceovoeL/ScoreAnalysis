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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yg.scoreanalysis.data.Aggregates
import com.yg.scoreanalysis.data.AppData
import com.yg.scoreanalysis.data.totalScore
import com.yg.scoreanalysis.ui.components.CardHeader
import com.yg.scoreanalysis.ui.components.ColorDot
import com.yg.scoreanalysis.ui.components.SectionCard
import com.yg.scoreanalysis.ui.components.SubjectScoreRow
import com.yg.scoreanalysis.ui.components.Tag
import com.yg.scoreanalysis.ui.theme.subjectColor

/** 记录：按时间倒序展示所有考试组与单科考试 */
@Composable
fun RecordsScreen(
    data: AppData,
    onOpenGroup: (String) -> Unit,
    onOpenSubject: (String) -> Unit,
) {
    val groups = Aggregates.groupsOf(data).reversed()
    val standalone = data.exams.sortedByDescending { it.date }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("记录", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "共 " + (groups.size + standalone.size) + " 条",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (groups.isEmpty() && standalone.isEmpty()) {
            item {
                SectionCard(Modifier.fillMaxWidth()) {
                    Text(
                        "还没有记录。在「总览」里点「记一次考试组」开始。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (groups.isNotEmpty()) {
            item {
                Text(
                    "考试组",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                )
            }
            items(groups.size) { i ->
                val g = groups[i]
                SectionCard(Modifier.fillMaxWidth(), onClick = { onOpenGroup(g.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            g.name.ifBlank { "未命名考试组" },
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            fmtNum(g.totalScore) + " / " + fmtNum(g.totalFullScore),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        g.entries.take(3).forEach { e ->
                            val seed = data.subjects.firstOrNull { it.id == e.subjectId }?.colorSeed ?: 0
                            Tag(e.subjectName, color = subjectColor(seed))
                        }
                        if (g.entries.size > 3) Tag("+" + (g.entries.size - 3))
                    }
                    val cr = g.classRank ?: g.totalRank
                    if (cr != null || g.gradeRank != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            buildString {
                                cr?.let { append("班级第 "); append(it); append(" 名") }
                                if (cr != null && g.gradeRank != null) append("  ·  ")
                                g.gradeRank?.let { append("年级第 "); append(it); append(" 名") }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (standalone.isNotEmpty()) {
            item {
                Text(
                    "单科考试",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }
            items(standalone.size) { i ->
                val e = standalone[i]
                val subject = data.subjects.firstOrNull { it.id == e.subjectId }
                val color = subjectColor(subject?.colorSeed ?: 0)
                SectionCard(
                    Modifier.fillMaxWidth(),
                    onClick = { onOpenSubject(e.subjectId) },
                ) {
                    SubjectScoreRow(
                        name = (subject?.name ?: "已删除科目") + " · " + e.name.ifBlank { "未命名" },
                        score = fmtNum(e.score),
                        full = fmtNum(e.examFullScore),
                        rank = e.rank,
                        color = color,
                    )
                }
            }
        }
    }
}
