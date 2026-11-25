package com.yg.scoreanalysis.ui.chart

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * 同一页面内所有图表共享的浮窗选中状态。
 * 只允许一个图表同时展示浮窗：任一图表打开时，其它图表自动关闭。
 */
class ChartSelection {
    /** 当前打开浮窗的图表 id；null 表示没有 */
    val owner: MutableState<String?> = mutableStateOf(null)
    /** 该图表内选中的点下标 */
    val index: MutableState<Int?> = mutableStateOf(null)

    fun open(chartId: String, i: Int) {
        owner.value = chartId
        index.value = i
    }

    fun close() {
        owner.value = null
        index.value = null
    }

    fun indexFor(chartId: String): Int? = if (owner.value == chartId) index.value else null

    fun isOpen(chartId: String): Boolean = owner.value == chartId && index.value != null
}
