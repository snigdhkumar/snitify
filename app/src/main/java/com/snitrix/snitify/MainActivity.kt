package com.snitrix.snitify

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.YouTubeLocale
import com.snitrix.snitify.data.repository.MusicRepository
import com.snitrix.snitify.playback.PlaybackManager
import com.snitrix.snitify.ui.screens.MainScreen
import com.snitrix.snitify.ui.theme.MetrolistSnitrixAndroidTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.snitrix.snitify.ui.viewmodel.MusicViewModel
import com.snitrix.snitify.ui.viewmodel.OnboardingViewModel
import com.snitrix.snitify.utils.cipher.CipherDeobfuscator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repository = MusicRepository()
                return MusicViewModel(repository, applicationContext) as T
            }
        }
    }

    private val onboardingViewModel: OnboardingViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OnboardingViewModel(applicationContext) as T
            }
        }
    }

    // Permission launcher for POST_NOTIFICATIONS (Android 13+)
    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize ThemeManager
        com.snitrix.snitify.ui.theme.ThemeManager.init(this)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Initialize CipherDeobfuscator (needed for WEB_REMIX stream decryption)
        CipherDeobfuscator.initialize(this)

        // Bootstrap InnerTube YouTube client locale
        YouTube.locale = YouTubeLocale(gl = "US", hl = "en")

        // Fetch visitorData on background so InnerTube requests are authenticated
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val visitorData = YouTube.visitorData().getOrNull()
                YouTube.visitorData = visitorData
                Timber.d("[Native] Initialized visitorData: ${visitorData?.take(20)}...")
            } catch (e: Exception) {
                Timber.e(e, "[Native] Failed to get visitor data")
            }
        }

        // Initialize PlaybackManager with ExoPlayer
        PlaybackManager.init(this)

        handleDownloadsIntent(intent)

        setContent {
            MetrolistSnitrixAndroidTheme {
                val isOnboardingFinished by onboardingViewModel.isOnboardingFinished.collectAsState()

                when (isOnboardingFinished) {
                    null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0B0B0B))
                        )
                    }
                    false -> {
                        com.snitrix.snitify.ui.screens.onboarding.OnboardingScreen(
                            viewModel = onboardingViewModel,
                            onOnboardingFinished = {
                                onboardingViewModel.finishOnboarding()
                            }
                        )
                    }
                    true -> {
                        MainScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDownloadsIntent(intent)
    }

    private fun handleDownloadsIntent(intent: android.content.Intent?) {
        if (intent?.getBooleanExtra("navigate_to_downloads", false) == true) {
            viewModel.selectTab("library")
            viewModel.selectLibraryCategory("downloads")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PlaybackManager.release()
    }
}