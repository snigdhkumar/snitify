package com.snitrix.snitify.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.snitrix.snitify.R

@Composable
fun DownloadThumbnailOverlay(
    progressPercent: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background ring track
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = Color.White.copy(alpha = 0.25f),
                strokeWidth = 2.5.dp,
                trackColor = Color.Transparent
            )
            // Active progress arc
            CircularProgressIndicator(
                progress = { (progressPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize(),
                color = Color.White,
                strokeWidth = 2.5.dp,
                trackColor = Color.Transparent
            )
            // Centered down-arrow icon
            Icon(
                painter = painterResource(id = R.drawable.ic_download),
                contentDescription = "Downloading",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
