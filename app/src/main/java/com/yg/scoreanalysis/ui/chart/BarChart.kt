package com.yg.scoreanalysis.ui.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

/**
 * 柱状图：用于"哪个分数考得最多"。
 * @param entries 分数 -> 次数
 * @param highlight 需要高亮的分数（最高频次）
 */
@Composable
fun BarChart(
    entries: List<Pair<Int, Int>>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    highlightColor: Color = MaterialTheme.colorScheme.secondary,
    highlight: Int? = null,
    animate: Boolean = true,
    chartHeight: androidx.compose.ui.unit.Dp = 190.dp,
) {
    if (entries.isEmpty()) {
        EmptyChartHint(modifier)
        return
    }
    val textMeasurer = rememberTextMeasurer()
    val progress = remember(entries, highlight) { Animatable(if (animate) 0f else 1f) }

    LaunchedEffect(entries, highlight) {
        if (animate) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
        } else {
            progress.snapTo(1f)
        }
    }
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val axisStyle = TextStyle(fontSize = 10.sp, color = labelColor)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
    ) {
        val leftPad = 30.dp.toPx()
        val rightPad = 10.dp.toPx()
        val topPad = 20.dp.toPx()
        val bottomPad = 26.dp.toPx()
        val plotW = size.width - leftPad - rightPad
        val plotH = size.height - topPad - bottomPad
        if (plotW <= 0f || plotH <= 0f) return@Canvas

        val maxCount = max(1, entries.maxOf { it.second })

        // 网格：次数是整数，刻度不能出现重复标签
        val gridSteps = if (maxCount <= 4) maxCount else 4
        for (i in 0..gridSteps) {
            val y = topPad + plotH * i / gridSteps.toFloat()
            drawLine(gridColor, Offset(leftPad, y), Offset(size.width - rightPad, y), 1.dp.toPx() * 0.7f)
            val v = maxCount - maxCount * i / gridSteps.toFloat()
            val label = Math.round(v).toString()
            val t = textMeasurer.measure(label, axisStyle)
            drawText(t, topLeft = Offset(leftPad - t.size.width - 4.dp.toPx(), y - t.size.height / 2f))
        }

        val n = entries.size
        val slot = plotW / n
        val barW = (slot * 0.56f).coerceAtMost(38.dp.toPx()).coerceAtLeast(6.dp.toPx())

        entries.forEachIndexed { i, (score, count) ->
            val cx = leftPad + slot * (i + 0.5f)
            val fullH = plotH * (count.toFloat() / maxCount)
            val h = fullH * progress.value
            val isHi = highlight != null && score == highlight
            val baseColor = if (isHi) highlightColor else color
            val left = cx - barW / 2f
            val top = topPad + plotH - h
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(baseColor.copy(alpha = if (isHi) 1f else 0.85f), baseColor.copy(alpha = if (isHi) 0.75f else 0.45f)),
                    startY = top,
                    endY = topPad + plotH,
                ),
                topLeft = Offset(left, top),
                size = Size(barW, h),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            )
            if (count > 0 && progress.value > 0.6f) {
                val t = textMeasurer.measure(count.toString(), TextStyle(fontSize = 10.sp, color = labelColor))
                drawText(t, topLeft = Offset(cx - t.size.width / 2f, top - t.size.height - 3.dp.toPx()))
            }
            val st = textMeasurer.measure(score.toString(), axisStyle)
            drawText(
                st,
                topLeft = Offset(cx - st.size.width / 2f, topPad + plotH + 6.dp.toPx()),
            )
        }
    }
}
