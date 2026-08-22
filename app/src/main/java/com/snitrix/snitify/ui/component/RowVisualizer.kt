package com.snitrix.snitify.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.snitrix.snitify.ui.theme.LocalAppThemeColors

@Composable
fun RowVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = LocalAppThemeColors.current.primaryAccent
) {
    val transition = rememberInfiniteTransition(label = "VisualizerBarTransition")

    val bar1Height by transition.animateFloat(
        initialValue = 4f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2Height by transition.animateFloat(
        initialValue = 16f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3Height by transition.animateFloat(
        initialValue = 8f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier.padding(end = 8.dp)
    ) {
        val h1 = if (isPlaying) bar1Height.dp else 4.dp
        val h2 = if (isPlaying) bar2Height.dp else 12.dp
        val h3 = if (isPlaying) bar3Height.dp else 6.dp
        Box(modifier = Modifier.size(3.dp, h1).background(color, RoundedCornerShape(1.5.dp)))
        Box(modifier = Modifier.size(3.dp, h2).background(color, RoundedCornerShape(1.5.dp)))
        Box(modifier = Modifier.size(3.dp, h3).background(color, RoundedCornerShape(1.5.dp)))
    }
}
