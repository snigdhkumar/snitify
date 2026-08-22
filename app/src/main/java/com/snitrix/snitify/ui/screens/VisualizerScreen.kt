package com.snitrix.snitify.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snitrix.snitify.ui.theme.LocalAppThemeColors
import com.snitrix.snitify.ui.theme.TextPrimary
import com.snitrix.snitify.ui.theme.TextSecondary
import com.snitrix.snitify.ui.viewmodel.MusicViewModel
import kotlin.math.sin

@Composable
fun VisualizerScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val isPlaying by viewModel.isPlaying.collectAsState()

    // Setup animating phase to offset sine wave
    val infiniteTransition = rememberInfiniteTransition(label = "VisualizerTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 1500 else 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PhaseAnimation"
    )

    val appColors = com.snitrix.snitify.ui.theme.LocalAppThemeColors.current

    // Top theme gradient background
    val topGradient = Brush.verticalGradient(
        colors = listOf(appColors.gradientTop, appColors.gradientMid, Color.Transparent),
        startY = 0f,
        endY = 400f
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(appColors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .background(topGradient)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Live Visualizer",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isPlaying) "Synchronized with active playback" else "Playback paused",
                color = TextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Waveform Graphic canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(16.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val midY = canvasHeight / 2f

                // Draw multiple equalizer bars using sine waves
                val barCount = 42
                val barGap = 6f
                val totalGapWidth = barGap * (barCount - 1)
                val barWidth = (canvasWidth - totalGapWidth) / barCount

                for (i in 0 until barCount) {
                    // Compose sine waves to make a complex organic looking waveform
                    val xFraction = i.toFloat() / barCount
                    val sineVal = sin(xFraction * 4f * Math.PI.toFloat() + phase) * 0.4f +
                            sin(xFraction * 10f * Math.PI.toFloat() - phase * 1.5f) * 0.2f
                    
                    val multiplier = if (isPlaying) 0.8f else 0.15f
                    val waveHeight = (sineVal * midY * multiplier).coerceIn(-midY + 10f, midY - 10f)

                    val x = i * (barWidth + barGap)
                    val height = Math.abs(waveHeight) * 2f + 8f // minimum height of 8px
                    val y = midY - (height / 2f)

                    val barBrush = Brush.verticalGradient(
                        colors = listOf(appColors.primaryAccent, appColors.primaryAccentBright),
                        startY = y,
                        endY = y + height
                    )

                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, height),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
        }
    }
}
