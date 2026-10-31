package com.example.coflow.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun CoFlowIcon(
    name: String,
    modifier: Modifier = Modifier,
    color: Color,
    contentDescription: String? = null
) {
    Canvas(
        modifier = modifier.then(
            if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier
        )
    ) {
        val s = size.minDimension / 24f
        val strokeWidth = 1.6f * s
        val stroke = Stroke(width = strokeWidth)
        fun point(x: Float, y: Float) = Offset(x * s, y * s)

        when (name) {
            "back" -> {
                val path = Path().apply { moveTo(14f * s, 5f * s); lineTo(7f * s, 12f * s); lineTo(14f * s, 19f * s) }
                drawPath(path, color, style = stroke)
                drawLine(color, point(7f, 12f), point(21f, 12f), strokeWidth)
            }
            "search" -> {
                drawCircle(color, radius = 6f * s, center = point(10f, 10f), style = stroke)
                drawLine(color, point(15f, 15f), point(21f, 21f), strokeWidth)
            }
            "check" -> {
                val path = Path().apply { moveTo(5f * s, 12f * s); lineTo(10f * s, 17f * s); lineTo(19f * s, 7f * s) }
                drawPath(path, color, style = Stroke(width = 2.5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            "chevron" -> {
                val path = Path().apply { moveTo(9f * s, 5f * s); lineTo(16f * s, 12f * s); lineTo(9f * s, 19f * s) }
                drawPath(path, color, style = stroke)
            }
            "chevron_left" -> {
                val path = Path().apply { moveTo(15f * s, 5f * s); lineTo(8f * s, 12f * s); lineTo(15f * s, 19f * s) }
                drawPath(path, color, style = stroke)
            }
            "person" -> {
                drawCircle(color, radius = 3.3f * s, center = point(12f, 7f), style = stroke)
                val path = Path().apply { moveTo(5f * s, 21f * s); lineTo(5f * s, 18f * s); cubicTo(5f * s, 12f * s, 19f * s, 12f * s, 19f * s, 18f * s); lineTo(19f * s, 21f * s) }
                drawPath(path, color, style = stroke)
            }
            "folder" -> {
                val path = Path().apply { moveTo(3f * s, 6f * s); lineTo(9f * s, 6f * s); lineTo(11f * s, 8f * s); lineTo(21f * s, 8f * s); lineTo(21f * s, 19f * s); lineTo(3f * s, 19f * s); close() }
                drawPath(path, color, style = stroke)
            }
            "calendar" -> {
                drawRoundRect(color, topLeft = point(3f, 5f), size = Size(18f * s, 16f * s), cornerRadius = CornerRadius(2f * s), style = stroke)
                drawLine(color, point(3f, 10f), point(21f, 10f), strokeWidth)
                drawLine(color, point(8f, 3f), point(8f, 7f), strokeWidth)
                drawLine(color, point(16f, 3f), point(16f, 7f), strokeWidth)
                for (x in listOf(8f, 12f, 16f)) for (y in listOf(14f, 18f)) drawCircle(color, radius = 0.8f * s, center = point(x, y))
            }
            "bell" -> {
                val path = Path().apply { moveTo(5f * s, 16f * s); lineTo(7f * s, 13.5f * s); lineTo(7f * s, 10f * s); cubicTo(7f * s, 3.5f * s, 17f * s, 3.5f * s, 17f * s, 10f * s); lineTo(17f * s, 13.5f * s); lineTo(19f * s, 16f * s); close() }
                drawPath(path, color, style = stroke)
                drawLine(color, point(5f, 16f), point(19f, 16f), strokeWidth)
                drawArc(color, 0f, 180f, false, topLeft = point(9f, 15.5f), size = Size(6f * s, 5f * s), style = stroke)
            }
            "tasks" -> {
                drawRoundRect(color, topLeft = point(5f, 4f), size = Size(14f * s, 17f * s), cornerRadius = CornerRadius(1.5f * s), style = stroke)
                for (y in listOf(8f, 12f, 16f)) { drawLine(color, point(10f, y), point(16f, y), strokeWidth = 1.3f * s); drawCircle(color, radius = 0.8f * s, center = point(7.5f, y)) }
            }
            "edit" -> {
                val path = Path().apply { moveTo(3.5f * s, 20.5f * s); lineTo(3.5f * s, 16.5f * s); lineTo(15.5f * s, 4.5f * s); lineTo(16.5f * s, 3.5f * s); cubicTo(17.5f * s, 2.5f * s, 20.5f * s, 2.5f * s, 21.5f * s, 3.5f * s); cubicTo(22.5f * s, 4.5f * s, 22.5f * s, 7.5f * s, 21.5f * s, 8.5f * s); lineTo(8.5f * s, 20.5f * s); close() }
                drawPath(path, color, style = stroke)
                drawLine(color, point(14f, 6f), point(18f, 10f), strokeWidth)
                val innerPath = Path().apply { moveTo(4.5f * s, 16.5f * s); lineTo(7.5f * s, 19.5f * s); lineTo(3.5f * s, 20.5f * s); close() }
                drawPath(innerPath, color, style = stroke)
            }
            "folder_plus" -> {
                val path = Path().apply { moveTo(4f * s, 20f * s); lineTo(4f * s, 6f * s); lineTo(9f * s, 6f * s); lineTo(11f * s, 8f * s); lineTo(20f * s, 8f * s); lineTo(20f * s, 14f * s); moveTo(4f * s, 20f * s); lineTo(11f * s, 20f * s) }
                drawPath(path, color, style = stroke)
                drawLine(color, point(16.5f, 15.5f), point(16.5f, 22.5f), 2.2f * s, StrokeCap.Round); drawLine(color, point(13f, 19f), point(20f, 19f), 2.2f * s, StrokeCap.Round)
            }
            "task_plus" -> {
                val path = Path().apply { moveTo(5f * s, 20f * s); lineTo(5f * s, 6f * s); lineTo(9f * s, 6f * s); lineTo(9f * s, 4f * s); lineTo(15f * s, 4f * s); lineTo(15f * s, 6f * s); lineTo(19f * s, 6f * s); lineTo(19f * s, 14f * s); moveTo(9f * s, 7f * s); lineTo(15f * s, 7f * s); moveTo(5f * s, 20f * s); lineTo(11f * s, 20f * s) }
                drawPath(path, color, style = stroke)
                drawLine(color, point(16.5f, 14.5f), point(16.5f, 21.5f), 2.2f * s, StrokeCap.Round); drawLine(color, point(13f, 18f), point(20f, 18f), 2.2f * s, StrokeCap.Round)
            }
        }
    }
}