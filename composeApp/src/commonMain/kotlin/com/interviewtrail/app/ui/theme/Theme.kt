package com.interviewtrail.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Modern 2026 UI Theme:
 * Deep Slate Navy, Electric Teal/Emerald accents, crisp Pearl Slate backgrounds,
 * glassmorphic surfaces, subtle hairline borders, and smooth rounded geometry.
 */
private val SlateDark = Color(0xFF0F172A)
private val SlateNavy = Color(0xFF1E293B)
private val SlateSubtle = Color(0xFF334155)

private val TealPrimary = Color(0xFF0D9488)
private val TealLight = Color(0xFF14B8A6)
private val TealWash = Color(0xFFCCFBF1)
private val TealSurface = Color(0xFFF0FDFA)

private val RoyalBlue = Color(0xFF2563EB)
private val RoyalBlueWash = Color(0xFFEFF6FF)

private val BackgroundLight = Color(0xFFF8FAFC)
private val SurfaceLight = Color(0xFFFFFFFF)
private val SurfaceVariantLight = Color(0xFFF1F5F9)
private val BorderLight = Color(0xFFE2E8F0)
private val BorderSubtle = Color(0xFFF1F5F9)
private val TextMutedLight = Color(0xFF64748B)

private val BackgroundDark = Color(0xFF0B0F17)
private val SurfaceDark = Color(0xFF131A26)
private val SurfaceVariantDark = Color(0xFF1C2636)
private val BorderDark = Color(0xFF243042)
private val TextMutedDark = Color(0xFF94A3B8)

private val Light = lightColorScheme(
    primary = SlateDark,
    onPrimary = Color.White,
    primaryContainer = TealSurface,
    onPrimaryContainer = TealPrimary,
    secondary = TealPrimary,
    onSecondary = Color.White,
    secondaryContainer = TealWash,
    onSecondaryContainer = Color(0xFF115E59),
    tertiary = RoyalBlue,
    tertiaryContainer = RoyalBlueWash,
    onTertiaryContainer = Color(0xFF1E40AF),
    background = BackgroundLight,
    onBackground = SlateDark,
    surface = SurfaceLight,
    onSurface = SlateDark,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextMutedLight,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = SurfaceVariantLight,
    surfaceContainer = BackgroundLight,
    surfaceContainerHigh = BorderLight,
    outline = BorderLight,
    outlineVariant = BorderSubtle,
    error = Color(0xFFEF4444),
    onError = Color.White,
)

private val Dark = darkColorScheme(
    primary = TealLight,
    onPrimary = Color(0xFF022C22),
    primaryContainer = SurfaceVariantDark,
    onPrimaryContainer = TealWash,
    secondary = TealLight,
    onSecondary = Color(0xFF022C22),
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = TealWash,
    tertiary = Color(0xFF60A5FA),
    tertiaryContainer = Color(0xFF1E3A8A),
    onTertiaryContainer = Color(0xFFDBEAFE),
    background = BackgroundDark,
    onBackground = Color(0xFFF1F5F9),
    surface = SurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextMutedDark,
    surfaceContainerLowest = BackgroundDark,
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceVariantDark,
    surfaceContainerHigh = BorderDark,
    outline = BorderDark,
    outlineVariant = SurfaceVariantDark,
    error = Color(0xFFF87171),
    onError = Color.Black,
)

private val AppTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.0).sp),
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(lineHeight = 24.sp, letterSpacing = 0.15.sp),
        bodyMedium = bodyMedium.copy(lineHeight = 22.sp, letterSpacing = 0.1.sp),
        bodySmall = bodySmall.copy(lineHeight = 18.sp),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
        labelMedium = labelMedium.copy(fontWeight = FontWeight.Medium),
        labelSmall = labelSmall.copy(fontWeight = FontWeight.Medium),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun InterviewTrailTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
