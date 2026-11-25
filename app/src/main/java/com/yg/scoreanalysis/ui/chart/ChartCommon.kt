package com.yg.scoreanalysis.ui.chart

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import java.util.Locale
import kotlin.math.abs

/** 把 y 轴最大值向上取整到好看的刻度 */
fun niceAxisMax(maxValue: Double, targetTicks: Int = 4): Double {
    if (maxValue <= 0.0) return 1.0
    val rough = maxValue / targetTicks
    val magnitude = Math.pow(10.0, Math.floor(Math.log10(rough)))
    val normalized = rough / magnitude
    val niceUnit = when {
        normalized <= 1.0 -> 1.0
        normalized <= 2.0 -> 2.0
        normalized <= 5.0 -> 5.0
        else -> 10.0
    } * magnitude
    return niceUnit * Math.ceil(maxValue / niceUnit)
}

/** Catmull-Rom 平滑折线 */
fun buildSmoothPath(points: List<Offset>, tension: Float = 0.22f): Path {
    val path = Path()
    if (points.isEmpty()) return path
    if (points.size == 1) {
        path.moveTo(points[0].x, points[0].y)
        return path
    }
    path.moveTo(points[0].x, points[0].y)
    for (i in 0 until points.size - 1) {
        val p0 = points[if (i - 1 >= 0) i - 1 else i]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points[if (i + 2 < points.size) i + 2 else i + 1]
        val c1x = p1.x + (p2.x - p0.x) * tension
        val c1y = p1.y + (p2.y - p0.y) * tension
        val c2x = p2.x - (p3.x - p1.x) * tension
        val c2y = p2.y - (p3.y - p1.y) * tension
        path.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
    }
    return path
}

/** 折线与基线围成面积 */
fun buildAreaPath(linePath: Path, bottomY: Float, firstX: Float, lastX: Float): Path {
    val area = Path()
    area.addPath(linePath)
    area.lineTo(lastX, bottomY)
    area.lineTo(firstX, bottomY)
    area.close()
    return area
}

/** 按进度裁剪路径（描线动画） */
fun Path.extractPrefix(progress: Float): Path {
    if (progress >= 1f) return this
    val measure = PathMeasure().apply { setPath(this@extractPrefix, false) }
    val out = Path()
    measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), out, true)
    return out
}

fun Double.fmtScore(): String =
    if (abs(this - Math.round(this).toDouble()) < 0.05) Math.round(this).toString()
    else String.format(Locale.US, "%.1f", this)

fun Float.fmtScore(): String = toDouble().fmtScore()
