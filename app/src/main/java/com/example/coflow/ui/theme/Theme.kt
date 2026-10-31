package com.example.coflow.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// =====================================================
// ФОРМЫ — скругления компонентов
// =====================================================
object CoFlowShapes {
    val Card = RoundedCornerShape(24.dp)
    val Input = RoundedCornerShape(12.dp)
    val Button = RoundedCornerShape(14.dp)
}

// =====================================================
// ОТСТУПЫ — единые расстояния между элементами
// =====================================================
object CoFlowSpacing {
    val Small = 8.dp
    val Medium = 16.dp
    val Large = 24.dp

    val ScreenHorizontal = 20.dp
    val FieldStart = 16.dp
    val FieldGap = 4.dp
}

// =====================================================
// ЦВЕТОВЫЕ СХЕМЫ Material3
// =====================================================
private val DarkColorScheme = darkColorScheme(
    primary = CoFlowColors.Blue,
    secondary = CoFlowColors.BlueLight,
    tertiary = CoFlowColors.TextSecondary,
    background = Color(0xFF10213B),
    surface = Color(0xFF1E293B),
    onPrimary = CoFlowColors.White,
    onSecondary = CoFlowColors.White,
    onTertiary = CoFlowColors.White,
    onBackground = CoFlowColors.White,
    onSurface = CoFlowColors.White,
    error = CoFlowColors.Error
)

private val LightColorScheme = lightColorScheme(
    primary = CoFlowColors.Blue,
    secondary = CoFlowColors.BlueLight,
    tertiary = CoFlowColors.TextSecondary,
    background = CoFlowColors.BackgroundTop,
    surface = CoFlowColors.White,
    onPrimary = CoFlowColors.White,
    onSecondary = CoFlowColors.TextPrimary,
    onTertiary = CoFlowColors.TextPrimary,
    onBackground = CoFlowColors.TextPrimary,
    onSurface = CoFlowColors.TextPrimary,
    error = CoFlowColors.Error
)

// =====================================================
// КОМПОЗАБЛ ТЕМЫ
// =====================================================
@Composable
fun CoFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color доступен на Android 12+.
    // Установите false, если хотите всегда использовать фирменные цвета CoFlow.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}