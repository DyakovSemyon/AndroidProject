package com.example.coflow.ui.theme
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Создаем фабрику индикатора с голубым цветом
val CoFlowIndication: IndicationNodeFactory
    @Composable
    get() = ripple(color = CoFlowColors.BlueLight)
