package com.example.coflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coflow.ui.theme.CoFlowColors
import com.example.coflow.ui.theme.CoFlowGlassStyles
import com.example.coflow.ui.theme.CoFlowShapes
import com.example.coflow.ui.theme.coFlowAuthFieldColors

@Composable
fun CoFlowGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    leadingIcon: String? = null,
    trailingIcon: String? = null,
    singleLine: Boolean = true,
    maxLength: Int? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onClick: (() -> Unit)? = null // Если передан, поле работает как селектор (клик по всей области)
) {
    val isSelector = onClick != null

    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label,
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = CoFlowColors.TextPrimary
            )
        }

        val containerModifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 3.dp, shape = CoFlowShapes.Input, ambientColor = CoFlowGlassStyles.Shadow, spotColor = CoFlowGlassStyles.Shadow)
            .clip(CoFlowShapes.Input)
            .background(brush = Brush.verticalGradient(colors = listOf(Color.White.copy(alpha = 0.72f), Color(0xFFF5F8FF).copy(alpha = 0.58f))))
            .border(width = 1.dp, color = CoFlowGlassStyles.BlueBorder, shape = CoFlowShapes.Input)

        if (isSelector) {
            // 100% надежный вариант для селекторов: обычный Box с clickable.
            // Никаких капризов BasicTextField с перехватом фокуса.
            Box(
                modifier = containerModifier.clickable { onClick() }
            ) {
                TextFieldContent(
                    value = value,
                    placeholder = placeholder,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    singleLine = singleLine,
                    isSelector = true
                )
            }
        } else {
            // Стандартный вариант для редактируемых полей
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(if (maxLength != null) it.take(maxLength) else it) },
                modifier = containerModifier,
                textStyle = TextStyle(color = CoFlowColors.TextPrimary, fontSize = 14.sp, lineHeight = 20.sp),
                cursorBrush = SolidColor(CoFlowColors.Blue),
                singleLine = singleLine,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                decorationBox = { innerTextField ->
                    TextFieldContent(
                        value = value,
                        placeholder = placeholder,
                        leadingIcon = leadingIcon,
                        trailingIcon = trailingIcon,
                        singleLine = singleLine,
                        isSelector = false,
                        innerTextField = innerTextField,
                        maxLength = maxLength
                    )
                }
            )
        }
    }
}

// Вспомогательный компонент для отрисовки содержимого (иконки + текст)
@Composable
private fun TextFieldContent(
    value: String,
    placeholder: String,
    leadingIcon: String?,
    trailingIcon: String?,
    singleLine: Boolean,
    isSelector: Boolean,
    innerTextField: (@Composable () -> Unit)? = null,
    maxLength: Int? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (singleLine) 52.dp else 88.dp)
                .padding(horizontal = 14.dp, vertical = if (singleLine) 10.dp else 12.dp),
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top
        ) {
            if (leadingIcon != null) {
                CoFlowIcon(name = leadingIcon, modifier = Modifier.size(21.dp), color = CoFlowColors.TextPrimary)
                Spacer(Modifier.width(10.dp))
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart) {
                if (value.isEmpty()) {
                    Text(text = placeholder, color = CoFlowColors.TextSecondary, fontSize = 14.sp)
                }
                if (innerTextField != null) {
                    innerTextField()
                } else {
                    // Для селекторов просто выводим текст как есть
                    Text(text = value, color = CoFlowColors.TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }

            if (trailingIcon != null) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(Color.White.copy(alpha = 0.65f), CircleShape)
                        .border(1.dp, CoFlowGlassStyles.BlueBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CoFlowIcon(name = trailingIcon, modifier = Modifier.size(14.dp), color = CoFlowColors.Blue)
                }
            }
        }

        if (!isSelector && !singleLine && maxLength != null) {
            Text(
                text = "${value.length}/$maxLength",
                modifier = Modifier.align(Alignment.End).padding(top = 6.dp, end = 14.dp),
                fontSize = 11.sp,
                color = CoFlowColors.TextSecondary
            )
        }
    }
}

@Composable
fun CoFlowAuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        isError = isError,
        supportingText = supportingText,
        shape = CoFlowShapes.Input,
        colors = coFlowAuthFieldColors()
    )
}