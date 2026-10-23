package com.salaryflow.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp

// 翡翠绿主色（攒钱 / 涨工资），朱砂红做支出与强调，米白做纸感底色。
private val Jade = Color(0xFF0F7B63)
private val JadeLight = Color(0xFF6FD9BC)
private val JadeDeep = Color(0xFF063A30)
private val Vermilion = Color(0xFFE4572E)
private val Amber = Color(0xFFF2B33D)
private val Cream = Color(0xFFF7F5F0)
private val Ink = Color(0xFF14161A)

private val LightColors = lightColorScheme(
    primary = Jade,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDF3E7),
    onPrimaryContainer = Color(0xFF04352C),
    secondary = Vermilion,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCCF),
    onSecondaryContainer = Color(0xFF4A1505),
    tertiary = Color(0xFFB07A0B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE6B0),
    onTertiaryContainer = Color(0xFF3D2A00),
    background = Cream,
    onBackground = Ink,
    surface = Color(0xFFFFFCF7),
    onSurface = Ink,
    surfaceVariant = Color(0xFFECE9E1),
    onSurfaceVariant = Color(0xFF4B4A44),
    outline = Color(0xFFB6B2A7),
    outlineVariant = Color(0xFFD9D5CA),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = JadeLight,
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF005142),
    onPrimaryContainer = Color(0xFFCDF3E7),
    secondary = Color(0xFFFFB59B),
    onSecondary = Color(0xFF5A1B08),
    secondaryContainer = Color(0xFF7C2C12),
    onSecondaryContainer = Color(0xFFFFDCCF),
    tertiary = Color(0xFFF0C36B),
    onTertiary = Color(0xFF3E2E00),
    tertiaryContainer = Color(0xFF5B4300),
    onTertiaryContainer = Color(0xFFFFE6B0),
    background = Color(0xFF0F1211),
    onBackground = Color(0xFFE5E3DD),
    surface = Color(0xFF161A18),
    onSurface = Color(0xFFE5E3DD),
    surfaceVariant = Color(0xFF2A2E2B),
    onSurfaceVariant = Color(0xFFC8C6BE),
    outline = Color(0xFF8E8B82),
    outlineVariant = Color(0xFF3F423F),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black,
        fontSize = 46.sp,
        lineHeight = 52.sp,
        letterSpacing = (-1.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-1).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.3.sp,
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

/** 首页渐变大卡专用色。 */
object BrandColors {
    val heroStart = Color(0xFF0B5F50)
    val heroEnd = Color(0xFF149377)
    val heroAccent = Color(0xFF8FF0D2)
    val savings = Color(0xFF0F7B63)
    val expense = Color(0xFFE4572E)
    val income = Color(0xFFF2B33D)
}
