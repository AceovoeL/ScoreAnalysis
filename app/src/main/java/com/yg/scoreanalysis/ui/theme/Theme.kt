package com.yg.scoreanalysis.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = AccentOn,
    primaryContainer = AccentSoft,
    onPrimaryContainer = TextPrimary,
    secondary = Gold,
    onSecondary = Color(0xFF20180A),
    secondaryContainer = GoldSoft,
    onSecondaryContainer = Gold,
    tertiary = Success,
    background = BgBase,
    onBackground = TextPrimary,
    surface = BgCard,
    onSurface = TextPrimary,
    surfaceVariant = BgCardAlt,
    onSurfaceVariant = TextSecondary,
    outline = BgStroke,
    outlineVariant = BgStroke,
    error = Danger,
    onError = Color.White,
    errorContainer = DangerSoft,
    onErrorContainer = Danger,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF4C6FE8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE5FF),
    onPrimaryContainer = Color(0xFF0B1020),
    secondary = Color(0xFF9A6B10),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE7B8),
    onSecondaryContainer = Color(0xFF2A1F05),
    background = LightBg,
    onBackground = LightText,
    surface = LightCard,
    onSurface = LightText,
    surfaceVariant = Color(0xFFEFF3F8),
    onSurfaceVariant = LightTextSecondary,
    outline = LightStroke,
)

/** darkMode: 0=跟随系统 1=浅色 2=深色（默认深色更贴合本设计） */
@Composable
fun ScoreAnalysisTheme(darkMode: Int = 2, content: @Composable () -> Unit) {
    val dark = when (darkMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val colors = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography, shapes = AppShapes, content = content)
}
