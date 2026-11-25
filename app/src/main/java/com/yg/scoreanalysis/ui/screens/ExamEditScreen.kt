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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yg.scoreanalysis.data.Exam
import com.yg.scoreanalysis.data.Subject
import com.yg.scoreanalysis.ui.components.RoundedCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamEditScreen(
    subject: Subject,
    initial: Exam,
    onBack: () -> Unit,
    onSave: (Exam) -> Unit,
) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var scoreText by remember(initial.id) {
        mutableStateOf(if (initial.score > 0.0) trimNum(initial.score) else "")
    }
    var fullText by remember(initial.id) {
        mutableStateOf(if (initial.examFullScore > 0.0) trimNum(initial.examFullScore) else trimNum(subject.fullScore))
    }
    var rankText by remember(initial.id) { mutableStateOf(initial.rank?.toString() ?: "") }
    var date by remember(initial.id) { mutableStateOf(initial.date) }
    var showDatePicker by remember { mutableStateOf(false) }

    val score = scoreText.toDoubleOrNull()
    val full = fullText.toDoubleOrNull() ?: subject.fullScore
    val overFull = score != null && full > 0 && score > full
    val differs = full > 0 && kotlin.math.abs(full - subject.fullScore) > 1e-6
    val canSave = name.isNotBlank() && score != null && full > 0

    val dateFmt = remember { SimpleDateFormat("yyyy 年 M 月 d 日", Locale.CHINA) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "返回") }
            Text(
                if (initial.name.isBlank()) "记一场考试" else "编辑考试",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                RoundedCard(modifier = Modifier.fillMaxWidth(), corner = 22) {
                    Text(subject.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "科目满分 " + trimNum(subject.fullScore),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("考试名称") },
                    placeholder = { Text("如：期中考试") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = dateFmt.format(Date(date)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("日期") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Rounded.DateRange, contentDescription = "选择日期")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = scoreText,
                        onValueChange = { scoreText = filterNumber(it) },
                        label = { Text("成绩") },
                        isError = overFull,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = fullText,
                        onValueChange = { fullText = filterNumber(it) },
                        label = { Text("本场满分") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (overFull) {
                item {
                    Text(
                        "成绩高于本场满分，请检查是否填反。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (differs) {
                item {
                    RoundedCard(
                        modifier = Modifier.fillMaxWidth(),
                        corner = 18,
                        container = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    ) {
                        Text(
                            "本场满分与科目满分不同，图表里可以点「按比例换算」按得分率对比。",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = rankText,
                    onValueChange = { rankText = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("排名（可选）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        onSave(
                            initial.copy(
                                name = name.trim(),
                                date = date,
                                score = score ?: 0.0,
                                rank = rankText.toIntOrNull(),
                                examFullScore = full,
                            )
                        )
                    },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("保存")
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = it }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
        ) {
            DatePicker(state = state)
        }
    }
}

fun trimNum(v: Double): String =
    if (kotlin.math.abs(v - Math.round(v)) < 0.05) Math.round(v).toString()
    else String.format(Locale.US, "%.1f", v)

fun filterNumber(s: String): String {
    val filtered = s.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    if (firstDot < 0) return filtered.take(6)
    val head = filtered.substring(0, firstDot + 1)
    val tail = filtered.substring(firstDot + 1).replace(".", "")
    return (head + tail.take(2)).take(8)
}
