package com.yg.scoreanalysis.ui.screens

import java.util.Locale

/** 数值格式化：整数不带小数，其余保留一位 */
fun fmt(v: Double): String =
    if (kotlin.math.abs(v - Math.round(v)) < 0.05) Math.round(v).toString()
    else String.format(Locale.US, "%.1f", v)

/** 图表横轴短标签 */
fun shortLabel(name: String): String = if (name.length <= 5) name else name.take(4) + "…"

/** 与 trimNum 一致，供编辑页复用 */
fun trimNumber(v: Double): String =
    if (kotlin.math.abs(v - Math.round(v)) < 0.05) Math.round(v).toString()
    else String.format(Locale.US, "%.1f", v)
