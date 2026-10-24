package com.salaryflow.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.salaryflow.app.R

private val Forest = Color(0xFF155C4A)
private val ForestDeep = Color(0xFF103D33)
private val Moss = Color(0xFF5F8B72)
private val Clay = Color(0xFFC5573F)
private val Ochre = Color(0xFFB58437)
private val Paper = Color(0xFFF4F1E8)
private val PaperRaised = Color(0xFFFFFCF6)
private val Ink = Color(0xFF19211E)

private val AppNumberFont = FontFamily(
    Font(R.font.manrope_variable, weight = FontWeight.Normal),
    Font(R.font.manrope_variable, weight = FontWeight.Medium),
    Font(R.font.manrope_variable, weight = FontWeight.SemiBold),
    Font(R.font.manrope_variable, weight = FontWeight.Bold),
)

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Color(0xFFFFFCF6),
    primaryContainer = Color(0xFFDCEBE4),
    onPrimaryContainer = Color(0xFF12392F),
    secondary = Clay,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4E3DE),
    onSecondaryContainer = Color(0xFF5C2418),
    tertiary = Ochre,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E8CE),
    onTertiaryContainer = Color(0xFF4C360F),
    background = Paper,
    onBackground = Ink,
    surface = PaperRaised,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE9E5DB),
    onSurfaceVariant = Color(0xFF62635D),
    outline = Color(0xFFA8A49A),
    outlineVariant = Color(0xFFD9D4C8),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD5BD),
    onPrimary = Color(0xFF07382C),
    primaryContainer = Color(0xFF174D3E),
    onPrimaryContainer = Color(0xFFD7F3E7),
    secondary = Color(0xFFF0B4A5),
    onSecondary = Color(0xFF512015),
    secondaryContainer = Color(0xFF6B3023),
    onSecondaryContainer = Color(0xFFFFDAD0),
    tertiary = Color(0xFFE6C47C),
    onTertiary = Color(0xFF3D2B08),
    tertiaryContainer = Color(0xFF5A4217),
    onTertiaryContainer = Color(0xFFFFE4AC),
    background = Color(0xFF101512),
    onBackground = Color(0xFFE5E4DD),
    surface = Color(0xFF171D19),
    onSurface = Color(0xFFE5E4DD),
    surfaceVariant = Color(0xFF2A302C),
    onSurfaceVariant = Color(0xFFC5C8C1),
    outline = Color(0xFF8F938C),
    outlineVariant = Color(0xFF414741),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = AppNumberFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 46.sp,
        lineHeight = 50.sp,
        letterSpacing = (-1.6).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = AppNumberFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 37.sp,
        letterSpacing = (-1).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = AppNumberFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp,
    ),
)

@Composable
fun SalaryFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}

/** Shared brand colors for bespoke surfaces and status accents. */
object BrandColors {
    val heroStart = Color(0xFF103E34)
    val heroEnd = Color(0xFF1A5E4C)
    val heroAccent = Color(0xFFA9E0C8)
    val savings = Color(0xFF26775F)
    val expense = Clay
    val income = Ochre
    val ink = ForestDeep
    val paper = PaperRaised
    val muted = Moss
}
