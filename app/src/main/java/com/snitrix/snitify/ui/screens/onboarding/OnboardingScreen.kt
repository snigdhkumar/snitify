package com.snitrix.snitify.ui.screens.onboarding

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.snitrix.snitify.ui.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onOnboardingFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val selectedLanguages by viewModel.selectedLanguages.collectAsState()
    val selectedArtists by viewModel.selectedArtists.collectAsState()

    // Handle Android hardware back button to navigate to previous step
    BackHandler(enabled = currentStep > 0 && currentStep < 3) {
        viewModel.previousStep()
    }

    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInHorizontally(animationSpec = tween(250)) { it } + fadeIn(animationSpec = tween(250)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(250)) { -it } + fadeOut(animationSpec = tween(250)))
            } else {
                (slideInHorizontally(animationSpec = tween(250)) { -it } + fadeIn(animationSpec = tween(250)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(250)) { it } + fadeOut(animationSpec = tween(250)))
            }
        },
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0B)),
        label = "onboarding_step_transition"
    ) { step ->
        when (step) {
            0 -> WelcomeStep(
                onGetStartedClick = { viewModel.nextStep() }
            )
            1 -> LanguageStep(
                selectedLanguages = selectedLanguages,
                onToggleLanguage = { viewModel.toggleLanguage(it) },
                onBackClick = { viewModel.previousStep() },
                onContinueClick = { viewModel.nextStep() }
            )
            2 -> ArtistStep(
                selectedArtists = selectedArtists,
                selectedLanguages = selectedLanguages,
                onToggleArtist = { viewModel.toggleArtist(it) },
                onBackClick = { viewModel.previousStep() },
                onContinueClick = { viewModel.nextStep() }
            )
            3 -> PreparingFeedStep(
                onFinishOnboarding = {
                    viewModel.finishOnboarding()
                    onOnboardingFinished()
                }
            )
        }
    }
}
