package com.snitrix.snitify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snitrix.snitify.R
import com.snitrix.snitify.ui.theme.DividerColor
import com.snitrix.snitify.ui.theme.GlassBorder
import com.snitrix.snitify.ui.theme.GlassSurface
import com.snitrix.snitify.ui.theme.LocalAppThemeColors
import com.snitrix.snitify.ui.theme.TextPrimary
import com.snitrix.snitify.ui.theme.TextSecondary

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val time: String,
    val iconRes: Int
)

@Composable
fun NotificationScreen(
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppThemeColors.current
    val topGradient = Brush.verticalGradient(
        colors = listOf(appColors.gradientTop, appColors.gradientMid, Color.Transparent),
        startY = 0f,
        endY = 400f
    )

    val notifications = listOf(
        NotificationItem(
            id = "1",
            title = "New Release: Midnight City Lights",
            message = "Neon Wave just dropped their latest Synthwave single. Listen now!",
            time = "2 hours ago",
            iconRes = R.drawable.ic_play
        ),
        NotificationItem(
            id = "2",
            title = "Offline Mode Enabled",
            message = "All downloaded tracks are ready for offline playback.",
            time = "1 day ago",
            iconRes = R.drawable.ic_download
        ),
        NotificationItem(
            id = "3",
            title = "Welcome to Snitrix Music",
            message = "Enjoy high-quality streaming and a fully native, responsive experience.",
            time = "2 days ago",
            iconRes = R.drawable.ic_bell
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(appColors.background)
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
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Notifications",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(notifications, key = { it.id }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(GlassSurface)
                                .border(1.dp, GlassBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = item.iconRes),
                                contentDescription = null,
                                tint = appColors.primaryAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.message,
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.time,
                                color = TextSecondary.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }
                    Divider(color = DividerColor)
                }
            }
        }
    }
}
