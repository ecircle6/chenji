package com.birthapp.ui.theme

import androidx.compose.ui.graphics.Color

// === Coral + Mint Vibrant Palette ===

// Primary - Coral
val Coral500 = Color(0xFFFF6B6B)
val Coral400 = Color(0xFFFF8A8A)
val Coral300 = Color(0xFFFFABAB)
val Coral700 = Color(0xFFE05555)

// Secondary - Teal
val Teal500 = Color(0xFF00BFA5)
val Teal400 = Color(0xFF26D9C0)
val Teal300 = Color(0xFF80EFD9)
val Teal700 = Color(0xFF009688)

// Tertiary - Sunny Yellow
val SunnyYellow = Color(0xFFFFD93D)
val SunnyYellow300 = Color(0xFFFFE97A)
val SunnyYellow700 = Color(0xFFE6C235)

// 纪念日类型强调色 - Violet（结婚纪念）
val Violet500 = Color(0xFF7C6BFF)
val Violet300 = Color(0xFFB5ACFF)
val Violet700 = Color(0xFF5B4BC4)

// 庄重类型（缅怀）：用素净的灰蓝，不用暖调，避开庆祝感
val SlateInk = Color(0xFF5B6B7A)
val SlateInkLight = Color(0xFF9FB0BF)
val CardSlate = Color(0xFFEDF1F5)
val CardSlateDark = Color(0xFF232A30)

// Card backgrounds
val CardPeach = Color(0xFFFFF5EE)
val CardMint = Color(0xFFF0F8F0)
val CardLavender = Color(0xFFF5F0FF)
val CardToday = Color(0xFFFFFDE7)

// Surfaces
val WarmLight = Color(0xFFF8F7F4)
val WarmDark = Color(0xFF1A1A18)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF2A2A28)

// Text
val TextPrimary = Color(0xFF2D2D2D)
val TextSecondary = Color(0xFF8E8E8E)
val TextOnDark = Color(0xFFE8E8E6)
val TextOnDarkSecondary = Color(0xFF9E9E9C)

// Dark mode card variants
val CardPeachDark = Color(0xFF3A2820)
val CardMintDark = Color(0xFF1A3028)
val CardLavenderDark = Color(0xFF2A2040)
val CardTodayDark = Color(0xFF3A3520)

// 已暂停的卡片：纯中性灰，不能用 surfaceVariant（它被设成了暖棕）
val CardPaused = Color(0xFFEBEBEB)
val CardPausedDark = Color(0xFF262626)

// === Hero 聚焦卡渐变（效果图 home-redesign-mockup.html 定稿值）===
// 浅色/深色各一对：start → end。缅怀用冷灰蓝（不走暖调），其余按类型区分
val HeroBirthdayStart = Color(0xFFFFC98A)
val HeroBirthdayEnd = Color(0xFFFF8A5C)
val HeroBirthdayDarkStart = Color(0xFFB06A2E)
val HeroBirthdayDarkEnd = Color(0xFFC74F2A)

val HeroLoveStart = Color(0xFFB5ACFF)
val HeroLoveEnd = Color(0xFF7C6BFF)
val HeroLoveDarkStart = Color(0xFF6A5BC4)
val HeroLoveDarkEnd = Color(0xFF4A3B9E)

val HeroMemorialStart = Color(0xFF8E9EAB)
val HeroMemorialEnd = Color(0xFF5B6B7A)
val HeroMemorialDarkStart = Color(0xFF4A545E)
val HeroMemorialDarkEnd = Color(0xFF31383F)

val HeroOtherStart = Color(0xFF5EEAD4)
val HeroOtherEnd = Color(0xFF00BFA5)
val HeroOtherDarkStart = Color(0xFF1E7A6E)
val HeroOtherDarkEnd = Color(0xFF0F5F54)
