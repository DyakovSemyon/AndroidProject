package com.example.coflow.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// =====================================================
// ЦВЕТА — единая палитра приложения CoFlow
// =====================================================
object CoFlowColors {
    // Основной синий: кнопки, курсор, активные элементы
    val Blue = Color(0xFF245BDB)
    val BlueLight = Color(0xFF8DB4FF)

    // Неактивная рамка полей ввода
    val BlueBorder = Color(0xFF9BBEFF)

    // Основной и второстепенный цвета текста
    val TextPrimary = Color(0xFF10213B)
    val TextSecondary = Color(0xFF64748B)

    val White = Color(0xFFFFFFFF)

    // Цвет ошибок валидации
    val Error = Color(0xFFB42318)

    // Остановка градиента сверху вниз: светлый верх → голубая середина → синий низ
    val BackgroundTop = Color(0xFFF9FBFF)
    val BackgroundLight = Color(0xFFE4EDFF)
    val BackgroundMiddle = Color(0xFFB9D1FF)
    val BackgroundBottom = Color(0xFF5791F8)

    // Полупрозрачный фон стеклянной карточки авторизации
    val CardBackground = Color(0x66DCE9FF)
}

// =====================================================
// ФОН — вертикальный градиент экрана
// =====================================================
fun coFlowBackgroundBrush(): Brush {
    return Brush.verticalGradient(
        colorStops = arrayOf(
            0.0f to CoFlowColors.BackgroundTop,
            0.22f to CoFlowColors.BackgroundLight,
            0.58f to CoFlowColors.BackgroundMiddle,
            1.0f to CoFlowColors.BackgroundBottom
        )
    )
}

fun coFlowTasksBackgroundBrush(): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF2F6FF),
            Color(0xFFEAF1FF),
            Color(0xFFEDF3FF)
        )
    )
}

// =====================================================
// ПОЛЯ ВВОДА — единое оформление OutlinedTextField
// =====================================================
@Composable
fun coFlowAuthFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = CoFlowColors.TextPrimary,
    unfocusedTextColor = CoFlowColors.TextPrimary,

    focusedContainerColor = CoFlowColors.White.copy(alpha = 0.35f),
    unfocusedContainerColor = CoFlowColors.White.copy(alpha = 0.28f),

    focusedBorderColor = CoFlowColors.Blue,
    unfocusedBorderColor = CoFlowColors.BlueBorder,

    focusedLabelColor = CoFlowColors.Blue,
    unfocusedLabelColor = CoFlowColors.TextPrimary,

    focusedPlaceholderColor = CoFlowColors.TextSecondary,
    unfocusedPlaceholderColor = CoFlowColors.TextSecondary,

    errorBorderColor = CoFlowColors.Error,
    errorLabelColor = CoFlowColors.Error,
    errorSupportingTextColor = CoFlowColors.Error
)

// =====================================================
// Glassmorphism-стили CoFlow (для Кири и Валеры)
// =====================================================
object CoFlowGlassStyles {
    val Surface = Color(0xBFFFFFFF)
    val ControlSurface = Color(0x99FFFFFF)
    val Border = Color(0xB3FFFFFF)
    val BlueBorder = Color(0x669BBEFF)
    val Shadow = Color(0x120E2C62)

    val CardShape = RoundedCornerShape(18.dp)
    val ControlShape = RoundedCornerShape(16.dp)
    val NavigationShape = RoundedCornerShape(22.dp)
    val SelectedItemShape = RoundedCornerShape(14.dp)
}