package com.sans.finance.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun Sparkline(
    data: List<Long>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    lineWidth: Float = 4f,
    showFill: Boolean = true,
    selectedIndex: Int = -1,
    onValueSelected: ((Int) -> Unit)? = null
) {
    if (data.size < 2) return

    val haptic = LocalHapticFeedback.current

    Canvas(
        modifier = modifier
            .then(
                if (onValueSelected != null) {
                    Modifier
                        .pointerInput(data) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val stepX = size.width / (data.size - 1)
                                    val index = (offset.x / stepX).toInt().coerceIn(0, data.size - 1)
                                    if (selectedIndex != index) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onValueSelected(index)
                                    }
                                },
                                onDrag = { change, _ ->
                                    val stepX = size.width / (data.size - 1)
                                    val index = (change.position.x / stepX).toInt().coerceIn(0, data.size - 1)
                                    if (selectedIndex != index) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onValueSelected(index)
                                    }
                                }
                            )
                        }
                        .pointerInput(data) {
                            detectTapGestures { offset ->
                                val stepX = size.width / (data.size - 1)
                                val index = (offset.x / stepX).toInt().coerceIn(0, data.size - 1)
                                if (selectedIndex != index) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onValueSelected(index)
                                }
                            }
                        }
                } else Modifier
            )
    ) {
        val width = size.width
        val height = size.height

        val minData = data.minOrNull() ?: 0L
        val maxData = data.maxOrNull() ?: 1L
        val range = (maxData - minData).coerceAtLeast(1L)

        val stepX = width / (data.size - 1)
        val points = data.mapIndexed { index, value ->
            val x = index * stepX
            val fractionY = (value - minData).toFloat() / range.toFloat()
            val y = height - (fractionY * (height - 8f)) - 4f
            Offset(x, y)
        }

        val path = Path()
        points.forEachIndexed { index, point ->
            if (index == 0) {
                path.moveTo(point.x, point.y)
            } else {
                val prev = points[index - 1]
                val cp1 = Offset(prev.x + (point.x - prev.x) / 2f, prev.y)
                val cp2 = Offset(prev.x + (point.x - prev.x) / 2f, point.y)
                path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, point.x, point.y)
            }
        }

        if (showFill) {
            val fillPath = Path().apply {
                addPath(path)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.25f), Color.Transparent)
                )
            )
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = lineWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        if (selectedIndex in points.indices) {
            val point = points[selectedIndex]

            // Vertical guideline
            drawLine(
                color = color.copy(alpha = 0.4f),
                start = Offset(point.x, 0f),
                end = Offset(point.x, height),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )

            // Halo outer ring
            drawCircle(
                color = color.copy(alpha = 0.25f),
                radius = lineWidth * 3.5f,
                center = point
            )

            // Main dot
            drawCircle(
                color = color,
                radius = lineWidth * 1.8f,
                center = point
            )

            // Center core
            drawCircle(
                color = Color.White,
                radius = lineWidth * 0.9f,
                center = point
            )
        }
    }
}

