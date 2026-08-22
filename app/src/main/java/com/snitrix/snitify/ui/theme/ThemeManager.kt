package com.snitrix.snitify.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTheme(val key: String, val displayName: String) {
    BLOOD_RED("blood_red", "Blood Red"),
    CLASSIC_PINK("classic_pink", "Classic Pink"),
    EMERALD_GREEN("emerald_green", "Emerald Green"),
    SAPPHIRE_BLUE("sapphire_blue", "Sapphire Blue"),
    GOLD_SUNSET("gold_sunset", "Gold Sunset"),
    OLED_PURE_BLACK("oled_pure_black", "OLED Pure Black")
}

enum class PlayerBackgroundStyle(val key: String, val displayName: String) {
    BLURRED_COVER("blurred", "Blurred Artwork"),
    DYNAMIC_GRADIENT("gradient", "Dynamic Gradient"),
    SOLID_DARK("solid", "Solid Dark")
}

data class AppThemeColors(
    val primaryAccent: Color,
    val primaryAccentBright: Color,
    val background: Color,
    val gradientTop: Color,
    val gradientMid: Color
)

val BloodRedThemeColors = AppThemeColors(
    primaryAccent = Color(0xFFE50914),
    primaryAccentBright = Color(0xFFFF2E3B),
    background = Color(0xFF0B0B0B),
    gradientTop = Color(0xFF000000),
    gradientMid = Color(0xFF000000)
)

val ClassicPinkThemeColors = AppThemeColors(
    primaryAccent = Color(0xFFFF4081),
    primaryAccentBright = Color(0xFFFF80AB),
    background = Color(0xFF0B0B0B),
    gradientTop = Color(0xFF000000),
    gradientMid = Color(0xFF000000)
)

val EmeraldGreenThemeColors = AppThemeColors(
    primaryAccent = Color(0xFF1DB954),
    primaryAccentBright = Color(0xFF1ED760),
    background = Color(0xFF0B0B0B),
    gradientTop = Color(0xFF000000),
    gradientMid = Color(0xFF000000)
)

val SapphireBlueThemeColors = AppThemeColors(
    primaryAccent = Color(0xFF1E88E5),
    primaryAccentBright = Color(0xFF42A5F5),
    background = Color(0xFF0B0B0B),
    gradientTop = Color(0xFF000000),
    gradientMid = Color(0xFF000000)
)

val GoldSunsetThemeColors = AppThemeColors(
    primaryAccent = Color(0xFFFFA000),
    primaryAccentBright = Color(0xFFFFB300),
    background = Color(0xFF0B0B0B),
    gradientTop = Color(0xFF000000),
    gradientMid = Color(0xFF000000)
)

val OledPureBlackThemeColors = AppThemeColors(
    primaryAccent = Color(0xFF1DB954),
    primaryAccentBright = Color(0xFF1ED760),
    background = Color(0xFF000000),
    gradientTop = Color(0xFF000000),
    gradientMid = Color(0xFF000000)
)

val LocalAppThemeColors = compositionLocalOf { EmeraldGreenThemeColors }

object ThemeManager {
    private const val PREFS_NAME = "metrolist_theme_prefs"
    private const val KEY_THEME = "selected_theme"
    private const val KEY_WAVY_SLIDER = "wavy_slider_enabled"
    private const val KEY_BG_STYLE = "player_bg_style"
    private const val KEY_THUMBNAIL_ROTATION = "thumbnail_rotation_enabled"

    private var prefs: SharedPreferences? = null

    private val _currentTheme = MutableStateFlow(AppTheme.EMERALD_GREEN)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    private val _themeColors = MutableStateFlow(EmeraldGreenThemeColors)
    val themeColors: StateFlow<AppThemeColors> = _themeColors.asStateFlow()

    private val _wavySliderEnabled = MutableStateFlow(false)
    val wavySliderEnabled: StateFlow<Boolean> = _wavySliderEnabled.asStateFlow()

    private val _thumbnailRotationEnabled = MutableStateFlow(true)
    val thumbnailRotationEnabled: StateFlow<Boolean> = _thumbnailRotationEnabled.asStateFlow()

    private val _playerBgStyle = MutableStateFlow(PlayerBackgroundStyle.DYNAMIC_GRADIENT)
    val playerBgStyle: StateFlow<PlayerBackgroundStyle> = _playerBgStyle.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            
            val savedThemeKey = prefs?.getString(KEY_THEME, AppTheme.EMERALD_GREEN.key) ?: AppTheme.EMERALD_GREEN.key
            val theme = AppTheme.entries.firstOrNull { it.key == savedThemeKey } ?: AppTheme.EMERALD_GREEN
            setThemeInternal(theme)

            val wavy = prefs?.getBoolean(KEY_WAVY_SLIDER, false) ?: false
            _wavySliderEnabled.value = wavy

            val rotation = prefs?.getBoolean(KEY_THUMBNAIL_ROTATION, true) ?: true
            _thumbnailRotationEnabled.value = rotation

            val bgKey = prefs?.getString(KEY_BG_STYLE, PlayerBackgroundStyle.DYNAMIC_GRADIENT.key) ?: PlayerBackgroundStyle.DYNAMIC_GRADIENT.key
            val bgStyle = PlayerBackgroundStyle.entries.firstOrNull { it.key == bgKey } ?: PlayerBackgroundStyle.DYNAMIC_GRADIENT
            _playerBgStyle.value = bgStyle
        }
    }

    fun setTheme(theme: AppTheme) {
        prefs?.edit()?.putString(KEY_THEME, theme.key)?.apply()
        setThemeInternal(theme)
    }

    fun setWavySliderEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_WAVY_SLIDER, enabled)?.apply()
        _wavySliderEnabled.value = enabled
    }

    fun setThumbnailRotationEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_THUMBNAIL_ROTATION, enabled)?.apply()
        _thumbnailRotationEnabled.value = enabled
    }

    fun setPlayerBgStyle(style: PlayerBackgroundStyle) {
        prefs?.edit()?.putString(KEY_BG_STYLE, style.key)?.apply()
        _playerBgStyle.value = style
    }

    private fun setThemeInternal(theme: AppTheme) {
        _currentTheme.value = theme
        _themeColors.value = when (theme) {
            AppTheme.BLOOD_RED -> BloodRedThemeColors
            AppTheme.CLASSIC_PINK -> ClassicPinkThemeColors
            AppTheme.EMERALD_GREEN -> EmeraldGreenThemeColors
            AppTheme.SAPPHIRE_BLUE -> SapphireBlueThemeColors
            AppTheme.GOLD_SUNSET -> GoldSunsetThemeColors
            AppTheme.OLED_PURE_BLACK -> OledPureBlackThemeColors
        }
    }
}
