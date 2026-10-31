package com.example.coflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coflow.ui.theme.CoFlowColors
import com.example.coflow.ui.theme.CoFlowGlassStyles
import com.example.coflow.ui.theme.CoFlowShapes

// 1. Главная кнопка действия
@Composable
fun CoFlowPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp, // Единственный параметр, который варьировался (48 или 52)
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(height),
        shape = CoFlowShapes.Button,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = CoFlowColors.Blue,
                contentColor = Color.White,
                disabledContainerColor = CoFlowColors.Blue.copy(alpha = 0.45f),
                disabledContentColor = Color.White,
            ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp, disabledElevation = 0.dp),
    ) {
        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

// 2. Плавающая кнопка добавления (FAB)
@Composable
fun CoFlowFabButton(
    onClick: () -> Unit,
    iconName: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(42.dp)
                .shadow(
                    elevation = 3.dp,
                    shape = CircleShape,
                    ambientColor = CoFlowGlassStyles.Shadow,
                    spotColor = CoFlowGlassStyles.Shadow,
                ).clip(CircleShape)
                .background(CoFlowColors.Blue)
                .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        CoFlowIcon(name = iconName, modifier = Modifier.size(24.dp), Color.White)
    }
}

// 3. Стеклянная кнопка-иконка (для колокольчика и т.д.)
@Composable
fun CoFlowGlassIconButton(
    iconName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 21.dp,
    containerSize: Dp = 38.dp,
) {
    Box(
        modifier =
            modifier
                .size(containerSize)
                .shadow(
                    elevation = 2.dp,
                    shape = CircleShape,
                    ambientColor = CoFlowGlassStyles.Shadow,
                    spotColor = CoFlowGlassStyles.Shadow,
                ).clip(CircleShape)
                .background(CoFlowGlassStyles.ControlSurface)
                .border(1.dp, CoFlowGlassStyles.Border, CircleShape)
                .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        CoFlowIcon(name = iconName, modifier = Modifier.size(iconSize), color = CoFlowColors.TextPrimary)
    }
}

/**
 * Новая синяя кнопка для сохранения изменений.
 */
@Composable
fun CoFlowEditSaveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = CoFlowShapes.Button,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = CoFlowColors.Blue,
                contentColor = Color.White,
                disabledContainerColor = CoFlowColors.Blue.copy(alpha = 0.45f),
            ),
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Новая универсальная белая кнопка с красным текстом.
 * Надпись задаётся снаружи: «Удалить», «Отмена» и т. д.
 */
@Composable
fun CoFlowEditDeleteButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = CoFlowShapes.Button,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Red,
                disabledContainerColor = Color.White.copy(alpha = 0.6f),
                disabledContentColor = Color.Red.copy(alpha = 0.5f),
            ),
        elevation =
            ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp,
                pressedElevation = 1.dp,
                disabledElevation = 0.dp,
            ),
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
