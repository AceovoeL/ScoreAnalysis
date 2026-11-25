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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Series(val name: String, val color: Color, val values: List<Double?>)

/** 多序列折线图：各科排名波动 */
@Composable
fun MultiLineChart(
    series: List<Series>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    reverseY: Boolean = true,
    animate: Boolean = true,
    hiddenNames: Set<String> = emptySet(),
    chartHeight: androidx.compose.ui.unit.Dp = 210.dp,
) {
    val visible = series.filter { it.name !in hiddenNames && it.values.any { v -> v != null } }
    if (visible.isEmpty()) {
        EmptyChartHint(modifier)
        return
    }
    val textMeasurer = rememberTextMeasurer()
    val progress = remember(visible, labels, reverseY) { Animatable(if (animate) 0f else 1f) }

    LaunchedEffect(visible, labels, reverseY) {
        if (animate) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
        } else {
            progress.snapTo(1f)
        }
    }
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val axisStyle = TextStyle(fontSize = 10.sp, color = labelColor)
    val p = progress

    Canvas(
        modifier = modifier.fillMaxWidth().height(chartHeight)
    ) {
        val leftPad = 34.dp.toPx()
        val rightPad = 12.dp.toPx()
        val topPad = 14.dp.toPx()
        val bottomPad = 26.dp.toPx()
        val plotW = size.width - leftPad - rightPad
        val plotH = size.height - topPad - bottomPad
        if (plotW <= 0f || plotH <= 0f) return@Canvas

        val allValues = visible.flatMap { s -> s.values.filterNotNull() }
        if (allValues.isEmpty()) return@Canvas
        val rawMax = niceAxisMax(allValues.maxOrNull() ?: 1.0)
        val count = maxOf(labels.size, visible.maxOf { it.values.size })

        fun xAt(i: Int): Float =
            if (count <= 1) leftPad + plotW / 2f else leftPad + plotW * i / (count - 1).toFloat()

        fun yAt(v: Double): Float {
            val t = (v / rawMax).coerceIn(0.0, 1.0).toFloat()
            val ty = if (reverseY) t else 1f - t
            return topPad + plotH * ty
        }

        for (i in 0..3) {
            val y = topPad + plotH * i / 3f
            drawLine(gridColor, Offset(leftPad, y), Offset(size.width - rightPad, y), 1.dp.toPx() * 0.7f)
            val v = rawMax - rawMax * i / 3
            val measured = textMeasurer.measure(v.fmtScore(), axisStyle)
            drawText(measured, topLeft = Offset(leftPad - measured.size.width - 4.dp.toPx(), y - measured.size.height / 2f))
        }

        visible.forEach { s ->
            val segment = ArrayList<Offset>()
            for (i in s.values.indices) {
                val v = s.values[i]
                if (v == null) {
                    if (segment.size > 1) drawSeg(segment, s.color, p.value)
                    segment.clear()
                } else {
                    segment.add(Offset(xAt(i), yAt(v)))
                }
            }
            // 只有一个点时画点，不画线
            if (segment.size == 1) {
                drawCircle(s.color, radius = 3.6.dp.toPx(), center = segment.first())
            } else if (segment.size > 1) {
                drawSeg(segment, s.color, p.value)
            }
            if (p.value > 0.9f) {
                s.values.forEachIndexed { i, v ->
                    if (v != null) drawCircle(s.color, radius = 2.6.dp.toPx(), center = Offset(xAt(i), yAt(v)))
                }
            }
        }

        val step = maxOf(1, Math.ceil(count / 5.0).toInt())
        for (i in labels.indices step step) {
            val measured = textMeasurer.measure(labels[i], axisStyle)
            val x = (xAt(i) - measured.size.width / 2f).coerceIn(0f, size.width - measured.size.width)
            drawText(measured, topLeft = Offset(x, topPad + plotH + 6.dp.toPx()))
        }
    }
}

private fun DrawScope.drawSeg(points: List<Offset>, color: Color, progress: Float) {
    val path = buildSmoothPath(points)
    drawPath(path.extractPrefix(progress), color = color, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))
}
