package com.snitrix.snitify.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.snitrix.snitify.ui.screens.settings.AboutScreen
import com.snitrix.snitify.ui.screens.settings.AppUpdateStep
import com.snitrix.snitify.ui.screens.settings.AppearanceScreen
import com.snitrix.snitify.ui.screens.settings.SettingsMainStep

enum class SettingsSubScreen {
    MAIN,
    APPEARANCE,
    ABOUT,
    APP_UPDATE
}

@Composable
fun SettingsScreen(
    onBackToHome: () -> Unit = {},
    viewModel: com.snitrix.snitify.ui.viewmodel.MusicViewModel? = null,
    isPlayerOpen: Boolean = false,
    modifier: Modifier = Modifier
) {
    var currentSubScreen by remember { mutableStateOf(SettingsSubScreen.MAIN) }

    // Handle Android back button when viewing sub-screens (only if Now Playing player is not open)
    BackHandler(enabled = currentSubScreen != SettingsSubScreen.MAIN && !isPlayerOpen) {
        currentSubScreen = SettingsSubScreen.MAIN
    }

    AnimatedContent(
        targetState = currentSubScreen,
        transitionSpec = {
            if (targetState != SettingsSubScreen.MAIN) {
                (slideInHorizontally(animationSpec = tween(200)) { it } + fadeIn(animationSpec = tween(200)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(200)) { -it } + fadeOut(animationSpec = tween(200)))
            } else {
                (slideInHorizontally(animationSpec = tween(200)) { -it } + fadeIn(animationSpec = tween(200)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(200)) { it } + fadeOut(animationSpec = tween(200)))
            }
        },
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0B)),
        label = "settings_subscreen_transition"
    ) { subScreen ->
        when (subScreen) {
            SettingsSubScreen.MAIN -> SettingsMainStep(
                onNavigateToAppearance = { currentSubScreen = SettingsSubScreen.APPEARANCE },
                onNavigateToAbout = { currentSubScreen = SettingsSubScreen.ABOUT },
                onNavigateToUpdate = { currentSubScreen = SettingsSubScreen.APP_UPDATE },
                onBackClick = onBackToHome,
                viewModel = viewModel
            )
            SettingsSubScreen.APPEARANCE -> AppearanceScreen(
                onBackClick = { currentSubScreen = SettingsSubScreen.MAIN }
            )
            SettingsSubScreen.ABOUT -> AboutScreen(
                onBackClick = { currentSubScreen = SettingsSubScreen.MAIN }
            )
            SettingsSubScreen.APP_UPDATE -> AppUpdateStep(
                onBackClick = { currentSubScreen = SettingsSubScreen.MAIN }
            )
        }
    }
}
