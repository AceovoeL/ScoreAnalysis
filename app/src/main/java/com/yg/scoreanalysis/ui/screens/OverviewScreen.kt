package com.yg.scoreanalysis.ui.screens

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yg.scoreanalysis.data.Aggregates
import com.yg.scoreanalysis.data.AppData
import com.yg.scoreanalysis.data.Exam
import com.yg.scoreanalysis.data.totalScore
import com.yg.scoreanalysis.ui.chart.LineChart
import com.yg.scoreanalysis.ui.components.BigValue
import com.yg.scoreanalysis.ui.components.CardHeader
import com.yg.scoreanalysis.ui.components.ColorDot
import com.yg.scoreanalysis.ui.components.SectionCard
import com.yg.scoreanalysis.ui.components.Tag
import com.yg.scoreanalysis.ui.components.ThinProgress
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.rounded.ChevronRight
import com.yg.scoreanalysis.ui.theme.subjectColor
import java.util.Locale

fun fmtNum(v: Double): String =
    if (kotlin.math.abs(v - Math.round(v)) < 0.05) Math.round(v).toString()
    else String.format(Locale.US, "%.1f", v)

fun shortName(name: String): String = if (name.length <= 5) name else name.take(4) + "…"

/** 升降箭头 + 数值 */
@Composable
fun DeltaChip(
    delta: Double?,
    modifier: Modifier = Modifier,
    upColor: Color = Color(0xFF52D6A0),
    downColor: Color = Color(0xFFFF6B6B),
    unit: String = "",
    asRank: Boolean = false,
    decimals: Int = 1,
) {
    if (delta == null) {
        Text("—", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
        return
    }
    val up = delta > 0.0001
    val down = delta < -0.0001
    val c = when {
        up -> upColor
        down -> downColor
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = when {
        up -> Icons.Rounded.ArrowUpward
        down -> Icons.Rounded.ArrowDownward
        else -> Icons.Rounded.Remove
    }
    val text = if (asRank) {
        if (Math.abs(delta) < 0.0001) "持平" else Math.round(Math.abs(delta)).toString() + " 名"
    } else {
        if (Math.abs(delta) < 0.0001) "持平"
        else String.format(Locale.US, "%." + decimals + "f", Math.abs(delta)) + unit
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = c, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(2.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = c, fontWeight = FontWeight.Medium, fontSize = 11.sp)
    }
}

/** 总览 */
@Composable
fun OverviewScreen(
    data: AppData,
    onOpenGroup: (String) -> Unit,
    onAddGroup: () -> Unit,
    onAddSubject: () -> Unit,
    onOpenSubject: (String) -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenRecords: () -> Unit = {},
) {
    // 同页多个图表共享一个浮窗：打开新的会自动关掉旧的
    val chartSel = androidx.compose.runtime.remember { com.yg.scoreanalysis.ui.chart.ChartSelection() }
    val groups = Aggregates.groupsOf(data)
    val latest = groups.lastOrNull()
    val prevGroup = if (groups.size >= 2) groups[groups.size - 2] else null
    val target = latest?.totalFullScore ?: 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                // 隐藏入口：点标题进设置
                Text(
                    "成绩分析",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenSettings() }
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                )
                // 隐藏入口：点统计文字进记录页
                Text(
                    data.subjects.size.toString() + " 门科目 · " + data.exams.size +
                        " 场单科 · " + data.groups.size + " 次组考",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenRecords() }
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                )
            }
        }

        if (latest == null && data.subjects.isEmpty()) {
            item {
                SectionCard(Modifier.fillMaxWidth()) {
                    Text("还没有数据", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "先添加科目并设置目标分，或新建一次考试组录入多科成绩。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onAddSubject,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            modifier = Modifier.weight(1f),
                        ) { Text("添加科目") }
                        Button(onClick = onAddGroup, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                            Text("新建考试组")
                        }
                    }
                }
            }
        }

        if (latest != null) {
            item {
                SectionCard(Modifier.fillMaxWidth(), corner = 24) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenGroup(latest.id) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "最新总分 · " + latest.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Tag("满分 " + fmtNum(target), color = MaterialTheme.colorScheme.secondary)
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = "打开考试组",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        BigValue(
                            value = fmtNum(latest.totalScore),
                            unit = "/ " + fmtNum(target),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f),
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            val rate = if (target > 0) latest.totalScore / target else 0.0
                            Text(
                                String.format(Locale.US, "%.1f%%", rate * 100),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            DeltaChip(
                                delta = Aggregates.groupDeltaTotal(prevGroup, latest),
                                unit = " 分",
                                asRank = !Aggregates.nearlyEqual(prevGroup?.totalFullScore ?: 0.0, latest.totalFullScore),
                                decimals = 1,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    ThinProgress(
                        progress = if (target > 0) (latest.totalScore / target).toFloat() else 0f,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        val classRank = latest.classRank ?: latest.totalRank
                        val prevClassRank = prevGroup?.let { it.classRank ?: it.totalRank }
                        Column {
                            Text("班级排名", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    classRank?.let { "第 " + it + " 名" } ?: "—",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(4.dp))
                                DeltaChip(
                                    delta = if (classRank != null && prevClassRank != null) (prevClassRank - classRank).toDouble() else null,
                                    asRank = true,
                                )
                            }
                        }
                        Column {
                            Text("年级排名", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    latest.gradeRank?.let { "第 " + it + " 名" } ?: "—",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                                Spacer(Modifier.width(4.dp))
                                DeltaChip(
                                    delta = if (latest.gradeRank != null && prevGroup?.gradeRank != null)
                                        (prevGroup.gradeRank - latest.gradeRank).toDouble() else null,
                                    asRank = true,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 各科目标 + 最近成绩
        if (data.subjects.isNotEmpty()) {
            item {
                SectionCard(Modifier.fillMaxWidth()) {
                    CardHeader(
                        "各科目标与最近成绩",
                        trailing = {
                            val withGoal = data.subjects.count { it.targetScore > 0 }
                            if (withGoal == 0) Tag("点击科目行可编辑")
                        },
                    )
                    Spacer(Modifier.height(2.dp))
                    data.subjects.forEach { subject ->
                        val exams = Aggregates.examsOf(data, subject.id)
                        val (prevExam, lastExam) = Aggregates.lastTwo(exams)
                        val color = subjectColor(subject.colorSeed)
                        val goal = subject.targetScore
                        val latestScore = lastExam?.score
                        val rate = if (goal > 0 && latestScore != null) (latestScore / goal).toFloat() else null
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOpenSubject(subject.id) }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ColorDot(color)
                                Spacer(Modifier.width(9.dp))
                                Text(
                                    subject.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = "打开科目",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp).padding(start = 2.dp),
                                )
                                if (latestScore != null) {
                                    Text(
                                        fmtNum(latestScore),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = color,
                                    )
                                    Text(
                                        " / " + fmtNum(subject.fullScore),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    DeltaChip(
                                        delta = Aggregates.deltaScore(prevExam, lastExam),
                                        unit = " 分",
                                    )
                                } else {
                                    Text(
                                        "暂无成绩",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (goal > 0) {
                                Spacer(Modifier.height(5.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.weight(1f)) {
                                        ThinProgress(
                                            progress = rate ?: 0f,
                                            color = if ((rate ?: 0f) >= 1f) Color(0xFF52D6A0) else color,
                                            height = 5,
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "目标 " + fmtNum(goal) +
                                            if (rate != null) "（" + String.format(Locale.US, "%.0f%%", rate * 100) + "）" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if ((rate ?: 0f) >= 1f) Color(0xFF52D6A0) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            lastExam?.rank?.let { r ->
                                Spacer(Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "班级第 " + r + " 名",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    DeltaChip(delta = Aggregates.deltaRank(prevExam, lastExam)?.toDouble(), asRank = true)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 组考升降一览
        if (groups.size >= 2) {
            item {
                SectionCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpenRecords() },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "组考变化",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Tag(groups.size.toString() + " 次")
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = "查看全部记录",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    groups.takeLast(5).reversed().forEachIndexed { idx, g ->
                        val before = groups.indexOf(g).takeIf { it > 0 }?.let { groups[it - 1] }
                        val cr = g.classRank ?: g.totalRank
                        val pcr = before?.let { it.classRank ?: it.totalRank }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenGroup(g.id) }
                                .padding(vertical = 7.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                g.name.ifBlank { "未命名" },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                fmtNum(g.totalScore) + "/" + fmtNum(g.totalFullScore),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(Modifier.width(10.dp))
                            DeltaChip(delta = Aggregates.groupDeltaTotal(before, g), unit = " 分")
                            Spacer(Modifier.width(8.dp))
                            DeltaChip(
                                delta = if (cr != null && pcr != null) (pcr - cr).toDouble() else null,
                                asRank = true,
                            )
                        }
                    }
                }
            }
        }

        // 科目趋势入口
        if (data.subjects.isNotEmpty()) {
            item {
                SectionCard(Modifier.fillMaxWidth()) {
                    CardHeader("科目总览", trailing = { Tag("点图表看详情") })
                    Spacer(Modifier.height(2.dp))
                    data.subjects.forEach { subject ->
                        val exams = Aggregates.examsOf(data, subject.id)
                        if (exams.isEmpty()) return@forEach
                        val color = subjectColor(subject.colorSeed)
                        Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ColorDot(color)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    subject.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    exams.size.toString() + " 次",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            LineChart(
                                values = exams.map { it.score },
                                labels = exams.map { shortName(it.name) },
                                selection = chartSel,
                                chartId = "subject-" + subject.id,
                                extraLines = exams.map { e ->
                                    "第 " + (e.rank?.toString() ?: "—") + " 名 · " + fmtNum(e.score) + "/" + fmtNum(e.examFullScore)
                                },
                                color = color,
                                fixedMax = subject.fullScore.takeIf { it > 0 },
                                animate = false,
                                chartHeight = 132.dp,
                            )
                        }
                    }
                }
            }
        }

        // 总分趋势
        if (groups.size >= 2) {
            item {
                SectionCard(Modifier.fillMaxWidth()) {
                    CardHeader(
                        "近 " + groups.size + " 次总分",
                        trailing = { Tag("满分 " + fmtNum(groups.maxOf { it.totalFullScore })) },
                    )
                    Spacer(Modifier.height(6.dp))
                    LineChart(
                        values = groups.map { it.totalScore },
                        labels = groups.map { shortName(it.name) },
                        selection = chartSel,
                        chartId = "overview-total",
                        extraLines = groups.map { g ->
                            "班级第 " + (g.classRank ?: g.totalRank)?.toString().orEmpty().ifEmpty { "—" } +
                                " 名 · 年级第 " + (g.gradeRank?.toString() ?: "—") + " 名"
                        },
                        color = MaterialTheme.colorScheme.secondary,
                        fixedMax = groups.maxOf { it.totalFullScore }.takeIf { it > 0 },
                        animate = false,
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAddGroup,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("记考试组")
                }
                Button(
                    onClick = onAddSubject,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("加科目")
                }
            }
        }
    }
}
