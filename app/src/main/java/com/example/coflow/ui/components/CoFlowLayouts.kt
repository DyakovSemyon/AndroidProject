package com.example.coflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coflow.ui.theme.CoFlowColors
import com.example.coflow.ui.theme.CoFlowGlassStyles
import androidx.compose.ui.draw.shadow

@Composable
fun CoFlowScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null, // <-- Теперь необязательный параметр
    trailingContent: @Composable (() -> Unit)? = null // <-- Слот для правой части (например, колокольчик)
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 60.dp, bottom = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Кнопка назад рисуется ТОЛЬКО если передан onBackClick
            if (onBackClick != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    CoFlowIcon(name = "back", modifier = Modifier.size(22.dp), color = CoFlowColors.TextSecondary, contentDescription = "Назад")
                }
                Spacer(Modifier.width(4.dp))
            }

            Column {
                Text(
                    text = title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoFlowColors.TextPrimary
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = CoFlowColors.TextSecondary
                    )
                }
            }
        }

        // Правая часть (колокольчик или что-то еще)
        if (trailingContent != null) {
            trailingContent()
        }
    }
}

data class NavItem(val icon: String, val title: String, val selected: Boolean, val onClick: () -> Unit)

@Composable
fun CoFlowBottomNavigationBar(
    items: List<NavItem>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 278.dp)
            .shadow(elevation = 1.dp, shape = CoFlowGlassStyles.NavigationShape, ambientColor = CoFlowGlassStyles.Shadow.copy(alpha = 0.28f), spotColor = CoFlowGlassStyles.Shadow.copy(alpha = 0.28f))
            .clip(CoFlowGlassStyles.NavigationShape)
            .background(brush = Brush.verticalGradient(colors = listOf(CoFlowGlassStyles.ControlSurface.copy(alpha = 0.52f), CoFlowGlassStyles.ControlSurface.copy(alpha = 0.34f))), shape = CoFlowGlassStyles.NavigationShape)
            .border(1.dp, CoFlowGlassStyles.Border.copy(alpha = 0.68f), CoFlowGlassStyles.NavigationShape)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(CoFlowGlassStyles.SelectedItemShape)
                    .background(if (item.selected) CoFlowColors.Blue else Color.Transparent)
                    .clickable { item.onClick() }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CoFlowIcon(name = item.icon, modifier = Modifier.size(18.dp), color = if (item.selected) Color.White else CoFlowColors.TextPrimary)
                Spacer(Modifier.height(1.dp))
                Text(text = item.title, fontSize = 10.sp, color = if (item.selected) Color.White else CoFlowColors.TextPrimary, maxLines = 1)
            }
        }
    }
}