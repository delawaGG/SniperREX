package com.sniperrex.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val SniperScheme = darkColorScheme(
    primary = CyberLime,
    onPrimary = CosmicDark,
    secondary = CyberAqua,
    onSecondary = CosmicDark,
    tertiary = CyberCrimson,
    background = CosmicDark,
    onBackground = TextPrimary,
    surface = PanelDark,
    onSurface = TextPrimary,
    surfaceVariant = PanelElevated,
    onSurfaceVariant = TextSubtle,
    outline = BorderGray,
    error = CyberCrimson,
)

private val SniperTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, letterSpacing = 1.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
        fontSize = 20.sp, letterSpacing = 0.5.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
        fontSize = 13.sp, letterSpacing = 1.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal,
        fontSize = 11.sp, letterSpacing = 0.5.sp,
    ),
)

/** Selalu gelap — tema neon tidak punya varian terang. */
@Composable
fun SniperRexTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SniperScheme, typography = SniperTypography, content = content)
}
