package br.com.habil.carona.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.habil.carona.domain.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B7A75),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBFF0EA),
    onPrimaryContainer = Color(0xFF00201E),
    secondary = Color(0xFF4A6360),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E4),
    onSecondaryContainer = Color(0xFF05201D),
    tertiary = Color(0xFF4F5BD5),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE0E0FF),
    onTertiaryContainer = Color(0xFF0A0F6B),
    background = Color(0xFFF4F8F8),
    onBackground = Color(0xFF161D1C),
    surface = Color(0xFFF4F8F8),
    onSurface = Color(0xFF161D1C),
    surfaceVariant = Color(0xFFDAE5E3),
    onSurfaceVariant = Color(0xFF3F4947),
    outline = Color(0xFF6F7977),
    outlineVariant = Color(0xFFBEC9C7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEEF4F3),
    surfaceContainer = Color(0xFFE8EFEE),
    surfaceContainerHigh = Color(0xFFE2EAE9),
    surfaceContainerHighest = Color(0xFFDCE4E3),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FD9D1),
    onPrimary = Color(0xFF003734),
    primaryContainer = Color(0xFF00504C),
    onPrimaryContainer = Color(0xFFBFF0EA),
    secondary = Color(0xFFB0CCC8),
    onSecondary = Color(0xFF1B3532),
    secondaryContainer = Color(0xFF324B48),
    onSecondaryContainer = Color(0xFFCCE8E4),
    tertiary = Color(0xFFBDC2FF),
    onTertiary = Color(0xFF1A2399),
    tertiaryContainer = Color(0xFF3A44BA),
    onTertiaryContainer = Color(0xFFE0E0FF),
    background = Color(0xFF0B1413),
    onBackground = Color(0xFFDDE5E3),
    surface = Color(0xFF0B1413),
    onSurface = Color(0xFFDDE5E3),
    surfaceVariant = Color(0xFF3F4947),
    onSurfaceVariant = Color(0xFFBEC9C7),
    outline = Color(0xFF899391),
    outlineVariant = Color(0xFF3F4947),
    surfaceContainerLowest = Color(0xFF060E0D),
    surfaceContainerLow = Color(0xFF111C1B),
    surfaceContainer = Color(0xFF162120),
    surfaceContainerHigh = Color(0xFF1C2B2A),
    surfaceContainerHighest = Color(0xFF223433),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun CaronaTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
