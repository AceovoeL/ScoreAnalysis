package com.yg.scoreanalysis.ui.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 折线图。values 中 null 表示该点缺失（断线）。
 * reverseY=true 用于排名：1 名在最上方。
 */
@Composable
fun LineChart(
    values: List<Double?>,
    modifier: Modifier = Modifier,
    labels: List<String> = emptyList(),
    color: Color = MaterialTheme.colorScheme.primary,
    reverseY: Boolean = false,
    fixedMax: Double? = null,
    animate: Boolean = true,
    showDots: Boolean = true,
    chartHeight: androidx.compose.ui.unit.Dp = 196.dp,
    /** 每个点的附加说明（如"原始分 120/150"），显示在浮窗第三行 */
    extraLines: List<String> = emptyList(),
    /** 点击空白处是否清除选中 */
    dismissOnOutsideTap: Boolean = true,
    /** 同页多个图表共享的浮窗状态；为空则各图表独立 */
    selection: ChartSelection? = null,
    /** 本图表在共享状态中的标识 */
    chartId: String = "",
) {
    val ownIndex = remember(values) { mutableStateOf<Int?>(null) }
    val touchedIndexState = if (selection != null) {
        // 共享模式：只有 owner 是自己时才有选中点
        object : androidx.compose.runtime.State<Int?> {
            override val value: Int?
                get() = if (selection.owner.value == chartId) selection.index.value else null
        }
    } else ownIndex
    val touchedIndex by touchedIndexState
    fun setTouched(i: Int?) {
        if (selection != null) {
            if (i == null) selection.close()
            else selection.open(chartId, i)
        } else {
            ownIndex.value = i
        }
    }
    if (values.isEmpty() || values.all { it == null }) {
        EmptyChartHint(modifier)
        return
    }
    val textMeasurer = rememberTextMeasurer()
    val progress = remember(values, reverseY, fixedMax) { Animatable(if (animate) 0f else 1f) }


    LaunchedEffect(values, reverseY, fixedMax) {
        if (animate) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        } else {
            progress.snapTo(1f)
        }
    }
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val axisStyle = TextStyle(fontSize = 10.sp, color = labelColor)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
            .pointerInput(values) {
                fun indexAt(x: Float): Int {
                    val leftPad = 38.dp.toPx()
                    val rightPad = 12.dp.toPx()
                    val plotW = size.width - leftPad - rightPad
                    if (plotW <= 0f || values.isEmpty()) return 0
                    val ratio = ((x - leftPad) / plotW).coerceIn(0f, 1f)
                    return if (values.size == 1) 0 else Math.round(ratio * (values.size - 1))
                }

                // 只有"横向"拖动才由图表处理；纵向手势放行，保证外层列表能滚动
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val touchSlop = viewConfiguration.touchSlop
                    var accumulatedX = 0f
                    var accumulatedY = 0f
                    var isHorizontal: Boolean? = null
                    var pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) break
                        accumulatedX += change.positionChange().x
                        accumulatedY += change.positionChange().y
                        if (isHorizontal == null) {
                            if (kotlin.math.abs(accumulatedX) > touchSlop &&
                                kotlin.math.abs(accumulatedX) > kotlin.math.abs(accumulatedY)
                            ) {
                                isHorizontal = true
                            } else if (kotlin.math.abs(accumulatedY) > touchSlop) {
                                isHorizontal = false
                            }
                        }
                        if (isHorizontal == true) {
                            // 浮窗未打开才跟随拖动显示；已打开时拖动不做切换
                            if (touchedIndex == null) setTouched(indexAt(change.position.x))
                            change.consume()
                        } else if (isHorizontal == false) {
                            break
                        }
                    }
                }
            }
            // 点击单独一个 pointerInput：与上面的拖动检测互不阻塞
            .pointerInput(values, labels, dismissOnOutsideTap) {
                fun idxAt(x: Float): Int {
                    val leftPad = 38.dp.toPx()
                    val rightPad = 12.dp.toPx()
                    val plotW = size.width - leftPad - rightPad
                    if (plotW <= 0f || values.isEmpty()) return 0
                    val ratio = ((x - leftPad) / plotW).coerceIn(0f, 1f)
                    return if (values.size == 1) 0 else Math.round(ratio * (values.size - 1))
                }
                detectTapGestures(onTap = { off ->
                    val idx = idxAt(off.x)
                    if (selection != null) {
                        // 共享模式：本图已打开则关闭；否则直接接管（自动关掉别的图表的浮窗）
                        if (selection.owner.value == chartId && selection.index.value != null) {
                            selection.close()
                        } else {
                            selection.open(chartId, idx)
                        }
                    } else {
                        // 独立模式：已打开则关闭，否则打开
                        setTouched(if (touchedIndex != null) null else idx)
                    }
                })
            }
    ) {
        val leftPad = 38.dp.toPx()
        val rightPad = 12.dp.toPx()
        val topPad = 16.dp.toPx()
        val bottomPad = 26.dp.toPx()
        val plotW = size.width - leftPad - rightPad
        val plotH = size.height - topPad - bottomPad
        if (plotW <= 0f || plotH <= 0f) return@Canvas

        val nonNull = values.filterNotNull()
        val dataMax = nonNull.maxOrNull() ?: 1.0
        // 排名图：纵轴从第 1 名到最差名次；其余：从 0 到科目满分（数据溢出时才抬高）
        val axisMin: Double
        val axisMax: Double
        if (reverseY) {
            axisMin = 1.0
            axisMax = Math.ceil(dataMax).coerceAtLeast(2.0)
        } else {
            axisMin = 0.0
            axisMax = fixedMax?.let { if (dataMax > it) niceAxisMax(dataMax) else it }
                ?: niceAxisMax(dataMax)
        }
        val span = (axisMax - axisMin).let { if (it <= 0.0) 1.0 else it }
        val count = values.size

        fun xAt(i: Int): Float =
            if (count == 1) leftPad + plotW / 2f
            else leftPad + plotW * i / (count - 1).toFloat()

        // reverseY 时第 1 名在最上方，名次越大越往下
        fun yAt(v: Double): Float {
            val t = ((v - axisMin) / span).coerceIn(0.0, 1.0).toFloat()
            val ty = if (reverseY) t else 1f - t
            return topPad + plotH * ty
        }

        // 刻度数量：名次跨度小时少画几条，避免出现重复标签
        val ticks = if (reverseY) Math.min(4, Math.ceil(span).toInt().coerceAtLeast(1)) else 4
        for (i in 0..ticks) {
            val y = topPad + plotH * i / ticks.toFloat()
            drawLine(gridColor, Offset(leftPad, y), Offset(size.width - rightPad, y), 1.dp.toPx() * 0.7f)
            val value = if (reverseY) axisMin + span * i / ticks else axisMax - span * i / ticks
            val label = if (reverseY) Math.round(value).toString() else value.fmtScore()
            val measured = textMeasurer.measure(label, axisStyle)
            drawText(measured, topLeft = Offset(leftPad - measured.size.width - 4.dp.toPx(), y - measured.size.height / 2f))
        }

        val pts = ArrayList<Pair<Int, Offset>>()
        for (i in values.indices) {
            val v = values[i] ?: continue
            pts.add(i to Offset(xAt(i), yAt(v)))
        }
        if (pts.isEmpty()) return@Canvas

        val path = buildSmoothPath(pts.map { it.second })
        val drawn = path.extractPrefix(progress.value)
        // 单点时没有线段，给一个可视宽度让面积与点都看得见
        val firstX = pts.first().second.x
        val lastX = if (pts.size == 1) firstX + 1.dp.toPx() else pts.last().second.x
        val area = buildAreaPath(drawn, topPad + plotH, firstX, lastX)
        drawPath(
            area,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.02f)),
                startY = topPad,
                endY = topPad + plotH,
            )
        )
        drawPath(drawn, color = color, style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round))

        if (showDots && (progress.value > 0.98f || pts.size == 1)) {
            pts.forEach { (_, p) ->
                drawCircle(surfaceColor, radius = 4.2.dp.toPx(), center = p)
                drawCircle(color, radius = 2.8.dp.toPx(), center = p)
            }
        }

        val ti = touchedIndex
        if (ti != null && ti in values.indices && values[ti] != null) {
            val p = Offset(xAt(ti), yAt(values[ti]!!))
            drawLine(color.copy(alpha = 0.45f), Offset(p.x, topPad), Offset(p.x, topPad + plotH), 1.dp.toPx())
            drawCircle(color.copy(alpha = 0.20f), radius = 9.dp.toPx(), center = p)
            drawCircle(color, radius = 4.dp.toPx(), center = p)
            // 多行信息浮窗：名称 / 数值 / 排名
            val headText = labels.getOrNull(ti)?.takeIf { it.isNotBlank() } ?: ""
            val valueLine = (if (reverseY) "第 " else "") + values[ti]!!.fmtScore() + (if (reverseY) " 名" else "")
            val rankLine = extraLines.getOrNull(ti)
            val lines = listOfNotNull(
                headText.takeIf { it.isNotEmpty() },
                valueLine,
                rankLine?.takeIf { it.isNotBlank() },
            )
            val headStyle = TextStyle(fontSize = 10.sp, color = onPrimary.copy(alpha = 0.75f))
            val valueStyle = TextStyle(fontSize = 13.sp, color = onPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            val subStyle = TextStyle(fontSize = 10.sp, color = onPrimary.copy(alpha = 0.85f))
            val measuredLines = lines.mapIndexed { idx, t ->
                textMeasurer.measure(t, if (idx == 1) valueStyle else if (idx == 0) headStyle else subStyle)
            }
            val contentW = measuredLines.maxOf { it.size.width }
            val padH = 9.dp.toPx()
            val padV = 7.dp.toPx()
            val lineGap = 2.dp.toPx()
            val bubbleW = contentW + padH * 2
            val bubbleH = measuredLines.sumOf { it.size.height.toInt() }.toFloat() + lineGap * (lines.size - 1) + padV * 2
            val bx = (p.x - bubbleW / 2f).coerceIn(leftPad, (size.width - rightPad - bubbleW).coerceAtLeast(leftPad))
            var by = p.y - bubbleH - 14.dp.toPx()
            if (by < 0f) by = p.y + 14.dp.toPx()
            if (by + bubbleH > size.height) by = size.height - bubbleH - 2.dp.toPx()
            // 阴影 + 卡片底色
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.28f),
                topLeft = Offset(bx + 1.dp.toPx(), by + 2.dp.toPx()),
                size = Size(bubbleW, bubbleH),
                cornerRadius = CornerRadius(10.dp.toPx()),
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(bx, by),
                size = Size(bubbleW, bubbleH),
                cornerRadius = CornerRadius(10.dp.toPx()),
            )
            var ty = by + padV
            measuredLines.forEachIndexed { idx, m ->
                drawText(m, topLeft = Offset(bx + padH, ty))
                ty += m.size.height + lineGap
            }
        }

        if (labels.isNotEmpty()) {
            val step = maxOf(1, Math.ceil(count / 6.0).toInt())
            for (i in values.indices step step) {
                val label = labels.getOrNull(i) ?: continue
                val measured = textMeasurer.measure(label, axisStyle)
                val x = (xAt(i) - measured.size.width / 2f).coerceIn(0f, size.width - measured.size.width)
                drawText(measured, topLeft = Offset(x, topPad + plotH + 6.dp.toPx()))
            }
        }
    }
}

