package com.snitrix.snitify.ui.screens.settings.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.snitrix.snitify.R

@Composable
fun AppearancePreviewCard(
    modifier: Modifier = Modifier,
    isWavyEnabled: Boolean = true,
    label: String = "Wavy Seekbar Preview"
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141414))
            .border(1.5.dp, Color(0xFF282828), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = isWavyEnabled,
            animationSpec = tween(durationMillis = 300),
            label = "WavyPreviewCrossfade"
        ) { enabled ->
            Image(
                painter = painterResource(id = if (enabled) R.drawable.wavyseekbaron else R.drawable.wavyseekbaroff),
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
            )
        }
    }
}
