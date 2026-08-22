package com.snitrix.snitify.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snitrix.snitify.R
import com.snitrix.snitify.ui.screens.settings.components.ThemeCard
import com.snitrix.snitify.ui.theme.AppTheme
import com.snitrix.snitify.ui.theme.ThemeManager

data class ThemeOption(
    val theme: AppTheme,
    val name: String,
    val description: String,
    val previewColor: Color
)

val THEME_OPTIONS = listOf(
    ThemeOption(AppTheme.BLOOD_RED, "Blood Red", "Bold and energetic", Color(0xFFD32F2F)),
    ThemeOption(AppTheme.CLASSIC_PINK, "Classic Pink", "Soft and modern", Color(0xFFFF4081)),
    ThemeOption(AppTheme.EMERALD_GREEN, "Emerald", "Classic Snitify green", Color(0xFF1DB954)),
    ThemeOption(AppTheme.SAPPHIRE_BLUE, "Sapphire", "Deep blue tones", Color(0xFF1E88E5)),
    ThemeOption(AppTheme.GOLD_SUNSET, "Gold Sunset", "Warm golden glow", Color(0xFFFFA000)),
    ThemeOption(AppTheme.OLED_PURE_BLACK, "OLED Black", "Pure black for AMOLED displays", Color(0xFF121212))
)

@Composable
fun AppearanceScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTheme by ThemeManager.currentTheme.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0B))
            .statusBarsPadding()
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "App Theme & Appearance",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Themes",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose the look that matches your style.",
                        color = Color(0xFFA7A7A7),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            items(THEME_OPTIONS, key = { it.theme.name }) { option ->
                ThemeCard(
                    name = option.name,
                    description = option.description,
                    previewColor = option.previewColor,
                    isSelected = currentTheme == option.theme,
                    onClick = {
                        ThemeManager.setTheme(option.theme)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}
