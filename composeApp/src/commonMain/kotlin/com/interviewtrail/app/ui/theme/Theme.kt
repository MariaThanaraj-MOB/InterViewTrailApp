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
 * UI theme based on standard HTML wireframes and logo:
 * Crisp whites, dark navy accents, teal gradients, and light muted borders.
 */
private val NavyBlue = Color(0xFF123A5E)
private val NavyBlueDeep = Color(0xFF0B2A46)
private val Teal = Color(0xFF2FBF9E)
private val TealText = Color(0xFF1C8E76)
private val TealBg = Color(0xFFE3F7F1)
private val Cream = Color(0xFFF4F8F7)
private val Border = Color(0xFFDCE6E3)
private val Muted = Color(0xFF788088)
private val TextDark = Color(0xFF152534)
private val BackgroundMain = Color(0xFFD6DCDA)

private val Light = lightColorScheme(
    primary = NavyBlue, onPrimary = Color.White,
    primaryContainer = TealBg, onPrimaryContainer = TealText,
    secondary = Teal, onSecondary = Color.White,
    secondaryContainer = TealBg, onSecondaryContainer = NavyBlue,
    tertiary = NavyBlueDeep, tertiaryContainer = Border, onTertiaryContainer = TextDark,
    background = Cream, onBackground = TextDark,
    surface = Color.White, onSurface = TextDark,
    surfaceVariant = Color(0xFFFCFBF8), onSurfaceVariant = Muted,
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFCFBF8),
    surfaceContainer = Cream, surfaceContainerHigh = Border,
    outline = Border, outlineVariant = Color(0xFFEEE9DD),
    error = Color(0xFFCC1016),
)

private val Dark = darkColorScheme(
    primary = Teal, onPrimary = Color(0xFF000000),
    primaryContainer = NavyBlueDeep, onPrimaryContainer = TealBg,
    secondary = Color(0xFF457B9D), onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF122238), onSecondaryContainer = Teal,
    tertiary = Color(0xFF2F7B4C), tertiaryContainer = Color(0xFF133822), onTertiaryContainer = Color(0xFFD8ECD4),
    background = Color(0xFF1D2226), onBackground = Color(0xFFE9E9E9),
    surface = Color(0xFF1D2226), onSurface = Color(0xFFE9E9E9),
    surfaceVariant = Color(0xFF293138), onSurfaceVariant = Color(0xFFB1B3B6),
    surfaceContainerLowest = Color(0xFF1D2226), surfaceContainerLow = Color(0xFF293138),
    surfaceContainer = Color(0xFF1D2226), surfaceContainerHigh = Color(0xFF38434F),
    outline = Color(0xFF4A5568), outlineVariant = Color(0xFF293138),
    error = Color(0xFFE1696E),
)

private val AppTypography = Typography().run {
    copy(
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(lineHeight = 24.sp),
        bodyMedium = bodyMedium.copy(lineHeight = 21.sp),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Medium),
    )
}

private val AppShapes = Shapes(
    small = RoundedCornerShape(4.dp), // LinkedIn uses squarer corners
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
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
