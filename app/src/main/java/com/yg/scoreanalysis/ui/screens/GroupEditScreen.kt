package com.yg.scoreanalysis.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yg.scoreanalysis.data.ExamGroup
import com.yg.scoreanalysis.data.GroupEntry
import com.yg.scoreanalysis.data.Subject
import com.yg.scoreanalysis.ui.components.RoundedCard
import com.yg.scoreanalysis.ui.components.RoundedProgress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class EntryState(
    val score: String,
    val rank: String,
    val full: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupEditScreen(
    subjects: List<Subject>,
    initial: ExamGroup,
    onBack: () -> Unit,
    onSave: (ExamGroup) -> Unit,
) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var totalFullText by remember(initial.id) {
        mutableStateOf(if (initial.totalFullScore > 0) trimNum(initial.totalFullScore) else "")
    }
    var classRankText by remember(initial.id) {
        mutableStateOf((initial.classRank ?: initial.totalRank)?.toString() ?: "")
    }
    var gradeRankText by remember(initial.id) { mutableStateOf(initial.gradeRank?.toString() ?: "") }
    var date by remember(initial.id) { mutableStateOf(initial.date) }
    var showDatePicker by remember { mutableStateOf(false) }

    // subjectId -> 输入状态
    val entries = remember(initial.id) {
        mutableStateMapOf<String, EntryState>().apply {
            initial.entries.forEach { e ->
                put(
                    e.subjectId,
                    EntryState(
                        score = if (e.score > 0) trimNum(e.score) else "",
                        rank = e.rank?.toString() ?: "",
                        full = trimNum(e.fullScore),
                    )
                )
            }
            subjects.forEach { s ->
                if (!containsKey(s.id)) {
                    put(s.id, EntryState(score = "", rank = "", full = trimNum(s.fullScore)))
                }
            }
        }
    }

    // 明确记录"参与本次考试"的科目集合，避免用 score 是否为空来推断
    val included = remember(initial.id) {
        mutableStateMapOf<String, Boolean>().apply {
            subjects.forEach { s ->
                put(s.id, initial.entries.any { it.subjectId == s.id })
            }
        }
    }

    val totalFull = totalFullText.toDoubleOrNull() ?: 0.0
    val liveTotal = entries.entries
        .filter { (sid, _) -> included[sid] == true }
        .sumOf { (_, st) -> st.score.toDoubleOrNull() ?: 0.0 }
    val ratio = if (totalFull > 0) (liveTotal / totalFull).toFloat() else 0f
    val animatedRatio by animateFloatAsState(ratio.coerceIn(0f, 1f), tween(320), label = "liveTotal")

    val canSave = name.isNotBlank() && totalFull > 0 &&
        entries.any { (sid, st) -> included[sid] == true && st.score.toDoubleOrNull() != null }
    val dateFmt = remember { SimpleDateFormat("yyyy 年 M 月 d 日", Locale.CHINA) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "返回") }
            Text(
                if (initial.name.isBlank()) "新建考试组" else "编辑考试组",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
        }

        // 粘性实时总分条
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("实时总分", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Text(
                        trimNum(liveTotal) + if (totalFull > 0) " / " + trimNum(totalFull) else "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(6.dp))
                RoundedProgress(progress = animatedRatio)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    label = { Text("考试组名称") },
                    placeholder = { Text("如：高三第一次联考") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = dateFmt.format(Date(date)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("日期") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Rounded.DateRange, contentDescription = null)
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                    )
                    OutlinedTextField(
                        value = totalFullText,
                        onValueChange = { totalFullText = filterNumber(it) },
                        label = { Text("总分满分") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = classRankText,
                        onValueChange = { classRankText = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text("总分班级排名") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = gradeRankText,
                        onValueChange = { gradeRankText = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text("总分年级排名") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    "两项都可留空；下面各科填的排名按班级排名统计",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Text("各科成绩", style = MaterialTheme.typography.titleMedium)
                Text(
                    "勾选参与本次考试的科目，逐科填成绩与班级排名",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(subjects.size) { index ->
                val s = subjects[index]
                val st = entries[s.id] ?: EntryState("", "", trimNum(s.fullScore))
                val isOn = included[s.id] == true
                RoundedCard(
                    modifier = Modifier.fillMaxWidth(),
                    corner = 22,
                    container = if (isOn) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isOn,
                            onCheckedChange = { checked -> included[s.id] = checked },
                        )
                        Text(s.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(
                            "满分 " + trimNum(s.fullScore),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = st.score,
                            onValueChange = { v ->
                                entries[s.id] = st.copy(score = filterNumber(v))
                                if (v.isNotBlank()) included[s.id] = true
                            },
                            enabled = isOn,
                            label = { Text("成绩") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = st.rank,
                            onValueChange = { v ->
                                entries[s.id] = st.copy(rank = v.filter { c -> c.isDigit() }.take(5))
                            },
                            enabled = isOn,
                            label = { Text("班级排名") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        val list = subjects.mapNotNull { s ->
                            if (included[s.id] != true) return@mapNotNull null
                            val st = entries[s.id] ?: return@mapNotNull null
                            val sc = st.score.toDoubleOrNull() ?: return@mapNotNull null
                            GroupEntry(
                                subjectId = s.id,
                                subjectName = s.name,
                                score = sc,
                                rank = st.rank.toIntOrNull(),
                                fullScore = st.full.toDoubleOrNull() ?: s.fullScore,
                            )
                        }
                        onSave(
                            initial.copy(
                                name = name.trim(),
                                date = date,
                                totalFullScore = totalFull,
                                entries = list,
                                classRank = classRankText.toIntOrNull(),
                                gradeRank = gradeRankText.toIntOrNull(),
                                totalRank = null,
                            )
                        )
                    },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("保存考试组")
                }
            }
        }
    }

    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { date = it }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
        ) { DatePicker(state = dpState) }
    }
}
