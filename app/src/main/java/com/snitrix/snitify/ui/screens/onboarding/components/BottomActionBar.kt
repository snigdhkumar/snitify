package com.snitrix.snitify.ui.screens.onboarding.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomActionBar(
    buttonText: String = "Continue",
    enabled: Boolean,
    onContinueClick: () -> Unit,
    selectionCountText: String? = null,
    modifier: Modifier = Modifier
) {
    val buttonColor by animateColorAsState(
        targetValue = if (enabled) com.snitrix.snitify.ui.theme.LocalAppThemeColors.current.primaryAccent else Color(0xFF282828),
        animationSpec = tween(200),
        label = "buttonColor"
    )

    val textColor by animateColorAsState(
        targetValue = if (enabled) Color.White else Color(0xFF727272),
        animationSpec = tween(200),
        label = "textColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0B0B0B))
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (selectionCountText != null) Arrangement.SpaceBetween else Arrangement.Center
        ) {
            if (selectionCountText != null) {
                Text(
                    text = selectionCountText,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Button(
                onClick = { if (enabled) onContinueClick() },
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    disabledContainerColor = Color(0xFF282828),
                    contentColor = textColor,
                    disabledContentColor = Color(0xFF727272)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .then(if (selectionCountText != null) Modifier.width(160.dp) else Modifier.fillMaxWidth())
                    .height(56.dp)
            ) {
                Text(
                    text = buttonText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
