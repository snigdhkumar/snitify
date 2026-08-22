package com.snitrix.snitify.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.snitrix.snitify.data.model.Song

@Composable
fun PlaylistCollageCover(
    logoPath: String? = null,
    songs: List<Song> = emptyList(),
    modifier: Modifier = Modifier
) {
    // Shimmer animation — always declared unconditionally (used when no covers available)
    val infiniteTransition = rememberInfiniteTransition(label = "collageSkeleton")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "collageShimmerAlpha"
    )

    val customLogo = logoPath.takeIf { !it.isNullOrEmpty() }

    // Only use the first 4 songs that have a valid fully-resolved URL cover (http/https)
    val coverList = remember(songs) {
        songs
            .filter { song ->
                val cover = song.finalCover
                cover is String && (cover.startsWith("http://") || cover.startsWith("https://"))
            }
            .map { it.finalCover as String }
            .distinct()
            .take(4)
    }

    Box(
        modifier = modifier.background(Color(0xFF222222)),
        contentAlignment = Alignment.Center
    ) {
        when {
            customLogo != null -> {
                AsyncImage(
                    model = customLogo,
                    contentDescription = "Playlist Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            coverList.size >= 4 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f)) {
                        AsyncImage(
                            model = coverList[0],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        AsyncImage(
                            model = coverList[1],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    Row(modifier = Modifier.weight(1f)) {
                        AsyncImage(
                            model = coverList[2],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        AsyncImage(
                            model = coverList[3],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
            coverList.isNotEmpty() -> {
                AsyncImage(
                    model = coverList.first(),
                    contentDescription = "Playlist Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                AsyncImage(
                    model = com.snitrix.snitify.R.drawable.playlist,
                    contentDescription = "Default Playlist Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
