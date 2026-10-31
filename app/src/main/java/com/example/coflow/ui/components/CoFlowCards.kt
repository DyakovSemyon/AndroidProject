package com.example.coflow.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.example.coflow.ui.theme.CoFlowColors
import com.example.coflow.ui.theme.CoFlowGlassStyles
import com.example.coflow.ui.theme.CoFlowShapes

@Composable
fun CoFlowGlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = CoFlowGlassStyles.CardShape,
    isSelected: Boolean = false, // <-- НОВЫЙ ПАРАМЕТР
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    // Вычисляем цвет фона в зависимости от состояния
    val backgroundColor = if (isSelected) {
        CoFlowColors.Blue.copy(alpha = 0.12f) // Голубой фон при выборе
    } else {
        CoFlowGlassStyles.Surface // Дефолтный полупрозрачный белый фон
    }

    Column(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = shape, ambientColor = CoFlowGlassStyles.Shadow, spotColor = CoFlowGlassStyles.Shadow)
            .clip(shape)
            .background(backgroundColor) // <-- ПРИМЕНЯЕМ ВЫЧИСЛЕННЫЙ ФОН
            .border(1.dp, CoFlowGlassStyles.BlueBorder, shape)
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .then(clickModifier)
    ) {
        content()
    }
}

@Composable
fun CoFlowAuthCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = CoFlowShapes.Card,
        color = CoFlowColors.CardBackground,
        border = BorderStroke(1.dp, CoFlowColors.BlueBorder.copy(alpha = 0.75f)),
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp)) {
            content()
        }
    }
}