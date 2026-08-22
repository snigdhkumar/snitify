package com.snitrix.snitify.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun WavySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    isPlaying: Boolean = true,
    enabled: Boolean = true,
    strokeWidth: Dp = 4.dp,
    thumbRadius: Dp = 8.dp
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }
    val thumbRadiusPx = with(density) { thumbRadius.toPx() }

    val normalizedValue = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
        .coerceIn(0f, 1f)

    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(normalizedValue) }

    val displayValue = if (isDragging) dragValue else normalizedValue

    val transition = rememberInfiniteTransition(label = "wave_phase")
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) (2f * Math.PI.toFloat()) else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase_anim"
    )

    val activeColor = colors.activeTrackColor
    val inactiveColor = colors.inactiveTrackColor
    val thumbColor = colors.thumbColor

    val containerHeight = thumbRadius * 3

    val baseModifier = modifier
        .fillMaxWidth()
        .height(containerHeight)

    val interactiveModifier = if (enabled) {
        baseModifier
            .pointerInput(valueRange) {
                detectTapGestures { offset ->
                    val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                    val mappedValue = valueRange.start + newValue * (valueRange.endInclusive - valueRange.start)
                    onValueChange(mappedValue)
                    onValueChangeFinished?.invoke()
                }
            }
            .pointerInput(valueRange) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragValue = (offset.x / size.width).coerceIn(0f, 1f)
                        val mappedValue = valueRange.start + dragValue * (valueRange.endInclusive - valueRange.start)
                        onValueChange(mappedValue)
                    },
                    onDragEnd = {
                        isDragging = false
                        onValueChangeFinished?.invoke()
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragValue = (dragValue + dragAmount / size.width).coerceIn(0f, 1f)
                        val mappedValue = valueRange.start + dragValue * (valueRange.endInclusive - valueRange.start)
                        onValueChange(mappedValue)
                    }
                )
            }
    } else {
        baseModifier
    }

    Box(
        modifier = interactiveModifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val centerY = size.height / 2f
            val activeWidth = width * displayValue
            val amplitude = if (isPlaying) 6.dp.toPx() else 0f
            val wavelength = 28.dp.toPx()

            // Draw Active Wavy Track
            if (activeWidth > 0f) {
                val activePath = Path()
                activePath.moveTo(0f, centerY)

                var x = 0f
                val step = 2.dp.toPx()
                while (x <= activeWidth) {
                    val y = centerY + sin((x / wavelength) * (2f * Math.PI.toFloat()) - wavePhase) * amplitude
                    activePath.lineTo(x, y)
                    x += step
                }

                drawPath(
                    path = activePath,
                    color = activeColor,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }

            // Draw Inactive Straight Track
            if (activeWidth < width) {
                drawLine(
                    color = inactiveColor,
                    start = Offset(activeWidth, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }

            // Draw Thumb
            drawCircle(
                color = thumbColor,
                radius = thumbRadiusPx,
                center = Offset(activeWidth, centerY)
            )
        }
    }
}
