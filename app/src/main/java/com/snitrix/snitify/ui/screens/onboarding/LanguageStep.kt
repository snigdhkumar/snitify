package com.snitrix.snitify.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snitrix.snitify.ui.screens.onboarding.components.BottomActionBar
import com.snitrix.snitify.ui.screens.onboarding.components.LanguageCard
import com.snitrix.snitify.ui.screens.onboarding.components.StepHeader

@Composable
fun LanguageStep(
    selectedLanguages: Set<String>,
    onToggleLanguage: (String) -> Unit,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isContinueEnabled = selectedLanguages.isNotEmpty()
    var showAddLanguageSheet by remember { mutableStateOf(false) }

    // Combine default languages with any custom user-added languages
    val allAvailableLanguages = androidx.compose.runtime.remember(selectedLanguages) {
        val base = mutableListOf("Hindi", "English")
        selectedLanguages.forEach { lang ->
            if (!base.contains(lang)) {
                base.add(lang)
            }
        }
        base
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0B))
    ) {
        StepHeader(
            currentStep = 1,
            totalSteps = 3,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "What languages do\nyou enjoy?",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 36.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "We'll recommend music in the languages you love.",
                color = Color(0xFFA7A7A7),
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(24.dp))

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
            ) {
                items(allAvailableLanguages.size) { index ->
                    val langName = allAvailableLanguages[index]
                    val subtitle = when (langName.lowercase()) {
                        "hindi" -> "हिन्दी"
                        "english" -> "English"
                        else -> langName
                    }
                    val flag = when (langName.lowercase()) {
                        "hindi" -> "🇮🇳"
                        "english" -> "🇬🇧"
                        else -> "🎵"
                    }

                    LanguageCard(
                        title = langName,
                        subtitle = subtitle,
                        flagEmoji = flag,
                        isSelected = selectedLanguages.contains(langName),
                        onSelect = { onToggleLanguage(langName) }
                    )
                }
            }
        }

        BottomActionBar(
            buttonText = "Continue",
            enabled = isContinueEnabled,
            onContinueClick = onContinueClick
        )
    }
}
