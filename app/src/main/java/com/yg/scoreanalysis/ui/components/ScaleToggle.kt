package com.yg.scoreanalysis.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

/**
 * 「按比例换算」按钮：只在满分不一致时显示。
 * 只是一个临时开关，不写入任何持久化状态——离开页面即恢复。
 */
@Composable
fun ScaleToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
        label = "scaleBg",
    )
    val fg by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "scaleFg",
    )
    val scale by animateFloatAsState(
        targetValue = if (checked) 1.04f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 600f),
        label = "scalePop",
    )
    Surface(
        modifier = modifier
            .scale(scale)
            .clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(Icons.Rounded.SwapVert, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
            Text(
                "按比例换算",
                style = MaterialTheme.typography.labelSmall,
                color = fg,
            )
        }
    }
}
