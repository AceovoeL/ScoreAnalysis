package com.yg.scoreanalysis.ui.chart

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * 图表高度自适应：
 * - 手机竖屏 196dp 左右
 * - 横屏/矮屏压缩到 150dp，避免一屏只能看到一个图
 * - 平板放大到 260dp，避免图表太小
 */
@Composable
fun adaptiveChartHeight(base: Dp = 196.dp): Dp {
    return base
}

/** 根据可用宽高比挑选图表高度 */
@Composable
fun chartHeightFor(screenWidth: Dp, screenHeight: Dp, base: Dp = 196.dp): Dp {
    val isLandscape = screenWidth > screenHeight
    val isTablet = screenWidth >= 600.dp
    return when {
        isLandscape -> min(base.value, 168f).dp
        isTablet -> (base.value * 1.35f).dp
        else -> base
    }
}
