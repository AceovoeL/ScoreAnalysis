package com.yg.scoreanalysis.ui.theme

import androidx.compose.ui.graphics.Color

// ---- 深色为主的设计色板（参考深色卡片风） ----
val BgBase = Color(0xFF0B0F14)
val BgRaised = Color(0xFF141A21)
val BgCard = Color(0xFF1A212A)
val BgCardAlt = Color(0xFF212A35)
val BgStroke = Color(0xFF2B3644)

val TextPrimary = Color(0xFFEAF0F7)
val TextSecondary = Color(0xFF9AA7B6)
val TextTertiary = Color(0xFF64717F)

// 主强调：蓝紫
val Accent = Color(0xFF6C8CFF)
val AccentSoft = Color(0xFF2A3A63)
val AccentOn = Color(0xFF0B1020)

// 次强调：琥珀金（冠军/目标）
val Gold = Color(0xFFE8B458)
val GoldSoft = Color(0xFF33280F)

// 语义色
val Danger = Color(0xFFFF6B6B)
val DangerSoft = Color(0xFF3A1A1A)
val Success = Color(0xFF52D6A0)
val SuccessSoft = Color(0xFF123027)
val Warn = Color(0xFFF0A85A)

// 浅色模式（保留可用，但默认深色）
val LightBg = Color(0xFFF6F8FB)
val LightCard = Color(0xFFFFFFFF)
val LightStroke = Color(0xFFE2E8F0)
val LightText = Color(0xFF141A21)
val LightTextSecondary = Color(0xFF5B6775)

/** 科目配色（高饱和，深色背景上更醒目） */
val SubjectPalette = listOf(
    Color(0xFF6C8CFF), // 蓝紫
    Color(0xFFFF8A5C), // 橙
    Color(0xFF52D6A0), // 薄荷绿
    Color(0xFFFFC75C), // 金黄
    Color(0xFFB98CFF), // 紫
    Color(0xFF4FC3F7), // 天蓝
    Color(0xFFFF7BA9), // 粉
    Color(0xFF9CCC65), // 黄绿
)

fun subjectColor(seed: Int): Color =
    SubjectPalette[((seed % SubjectPalette.size) + SubjectPalette.size) % SubjectPalette.size]
