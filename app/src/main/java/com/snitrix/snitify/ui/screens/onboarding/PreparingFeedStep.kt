package com.snitrix.snitify.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snitrix.snitify.R
import com.snitrix.snitify.ui.screens.onboarding.components.StepHeader
import kotlinx.coroutines.delay

@Composable
fun PreparingFeedStep(
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    var revealedCount by remember { mutableIntStateOf(0) }

    val checklist = listOf(
        "Saving your preferences",
        "Finding your favorite artists",
        "Creating recommendations",
        "Loading your library"
    )

    // Sequential tick reveal timing
    LaunchedEffect(Unit) {
        delay(400)
        revealedCount = 1
        delay(600)
        revealedCount = 2
        delay(600)
        revealedCount = 3
        delay(600)
        revealedCount = 4
        delay(800)
        onFinishOnboarding()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0B))
    ) {
        StepHeader(
            currentStep = 3,
            totalSteps = 3,
            onBackClick = { /* No back on final step */ }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            // Illustration
            Image(
                painter = painterResource(id = R.drawable.settingfeed),
                contentDescription = "Setting up feed illustration",
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(240.dp),
                contentScale = ContentScale.Fit
            )

            // Headings
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Setting up\nyour feed",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 38.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Hang tight while we create something just for you.",
                    color = Color(0xFFA7A7A7),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }

            // Sequential Checklist Items
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                checklist.forEachIndexed { index, itemText ->
                    val isRevealed = index < revealedCount
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRevealed) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(com.snitrix.snitify.ui.theme.LocalAppThemeColors.current.primaryAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✓",
                                    color = Color.Black,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF282828))
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = itemText,
                            color = if (isRevealed) Color.White else Color(0xFF555555),
                            fontSize = 15.sp,
                            fontWeight = if (isRevealed) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
