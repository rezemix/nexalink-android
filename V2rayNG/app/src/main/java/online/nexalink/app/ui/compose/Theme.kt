package online.nexalink.app.ui.compose

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import online.nexalink.app.AppConfig
import online.nexalink.app.handler.MmkvManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Фирменные цвета NEXALINK — совпадают с сайтом (nexalink.online): тёмно-синий
// navy как primary, оранжевый accent как secondary.
private val LightColor = lightColorScheme(
    primary = Color(0xFF0A1F3A), // NEXALINK Navy
    onPrimary = Color(0xFFFFFFFF), // White
    primaryContainer = Color(0xFFE7EBF0), // Navy-soft (сайт: --navy-soft)
    onPrimaryContainer = Color(0xFF0A1F3A), // Navy
    secondary = Color(0xFFEE451B), // NEXALINK Orange (сайт: --accent)
    onSecondary = Color(0xFFFFFFFF), // White
    secondaryContainer = Color(0xFFFDEAE4), // Pale Orange (сайт: --accent-soft)
    onSecondaryContainer = Color(0xFF3A1206), // Dark Brown
    tertiary = Color(0xFF1A8A4A), // Green (успех/подключено)
    onTertiary = Color(0xFFFFFFFF), // White
    tertiaryContainer = Color(0xFFE7F5EC), // Light Green
    onTertiaryContainer = Color(0xFF00201A), // Dark Teal
    error = Color(0xFFC2362C), // Red (сайт: --danger)
    errorContainer = Color(0xFFFBECE9), // Light Red
    onError = Color(0xFFFFFFFF), // White
    onErrorContainer = Color(0xFF410002), // Dark Red
    background = Color(0xFFFAFAF8), // Off-white (сайт: --bg)
    onBackground = Color(0xFF0A1F3A), // Navy
    surface = Color(0xFFFFFFFF), // White
    onSurface = Color(0xFF0A1F3A), // Navy
    surfaceVariant = Color(0xFFECEAE5), // Light warm gray (сайт: --border)
    onSurfaceVariant = Color(0xFF6B6B6B), // Muted gray
    outline = Color(0xFFDDD9D2), // Border-strong
    outlineVariant = Color(0xFFECEAE5), // Border
    inverseSurface = Color(0xFF0A1F3A), // Navy
    inverseOnSurface = Color(0xFFF4EFF4), // Very Light Gray
    inversePrimary = Color(0xFFAEC0D6), // Muted navy-blue
    scrim = Color(0xFF000000), // Black
    surfaceTint = Color(0xFF0A1F3A), // Navy
    surfaceContainerLowest = Color(0xFFFFFFFF), // White
    surfaceContainerLow = Color(0xFFF8F8F6), // Very Light warm Gray
    surfaceContainer = Color(0xFFF1F1EE), // Light warm Gray
    surfaceContainerHigh = Color(0xFFEBEBE7), // Light warm Gray
    surfaceContainerHighest = Color(0xFFE5E5E1), // Light warm Gray
)

private val DarkColor = darkColorScheme(
    primary = Color(0xFF8FA8C9), // Muted light navy-blue
    onPrimary = Color(0xFF0A1F3A), // Navy
    primaryContainer = Color(0xFF1B3357), // Deep navy
    onPrimaryContainer = Color(0xFFDCE6F2), // Pale blue
    secondary = Color(0xFFFF6A3D), // Brighter orange for dark bg
    onSecondary = Color(0xFF3A1206), // Dark Brown
    secondaryContainer = Color(0xFF6F2C10), // Burnt orange
    onSecondaryContainer = Color(0xFFFFE0D3), // Pale Orange
    tertiary = Color(0xFF6FCB94), // Mint Green
    onTertiary = Color(0xFF00382E), // Dark Teal
    tertiaryContainer = Color(0xFF0F4A2E), // Deep green
    onTertiaryContainer = Color(0xFFA0F2D0), // Light Green
    error = Color(0xFFFFB4AB), // Light Red
    errorContainer = Color(0xFF93000A), // Dark Red
    onError = Color(0xFF690005), // Deep Red
    onErrorContainer = Color(0xFFFFDAD6), // Light Red
    background = Color(0xFF0A1626), // Near-navy black
    onBackground = Color(0xFFE6E9EE), // Light Gray-blue
    surface = Color(0xFF0F1E33), // Deep navy surface
    onSurface = Color(0xFFE6E9EE), // Light Gray-blue
    surfaceVariant = Color(0xFF23344D), // Muted navy
    onSurfaceVariant = Color(0xFFB7C2D0), // Light gray-blue
    outline = Color(0xFF3D4F6B), // Muted navy border
    outlineVariant = Color(0xFF23344D), // Muted navy
    inverseSurface = Color(0xFFE6E9EE), // Light Gray-blue
    inverseOnSurface = Color(0xFF0F1E33), // Deep navy
    inversePrimary = Color(0xFF0A1F3A), // Navy
    scrim = Color(0xFF000000), // Black
    surfaceTint = Color(0xFF8FA8C9), // Muted light navy-blue
    surfaceContainerLowest = Color(0xFF060D18), // Near-black navy
    surfaceContainerLow = Color(0xFF0C1729), // Dark navy
    surfaceContainer = Color(0xFF11203A), // Dark navy
    surfaceContainerHigh = Color(0xFF162945), // Dark navy
    surfaceContainerHighest = Color(0xFF1C3251), // Dark navy
)

// Semantic Colors
val colorPing = Color(0xFF1A8A4A) // Green
val colorPingRed = Color(0xFFC2362C) // Red
val colorConfigType = Color(0xFFEE451B) // Orange
val colorFabActive = Color(0xFFEE451B) // Orange
val colorFabInactiveLight = Color(0xFF9C9C9C) // Gray
val colorFabInactiveDark = Color(0xFF646464) // Dark Gray
val dividerColorLight = Color(0xFFE0E0E0) // Light Gray
val dividerColorDark = Color(0xFF424242) // Dark Gray

// Большая кнопка подключения на главном экране.
val colorConnected = Color(0xFF1DB954)      // Зелёный — подключено
val colorConnectedDark = Color(0xFF17A34A)
val colorConnecting = Color(0xFFEE451B)     // Оранжевый (акцент) — идёт подключение
val colorDisconnectedLight = Color(0xFF0A1F3A) // Navy — отключено (светлая тема)
val colorDisconnectedDark = Color(0xFF3D4F6B)   // Приглушённый navy — отключено (тёмная тема)

// Toast Colors 70%
val toastNormalBgLight = Color(0xB3353A3E) // Dark Gray
val toastNormalBgDark = Color(0xB34A4F54) // Darker Gray
val toastSuccessBg = Color(0xB3388E3C) // Green
val toastErrorBg = Color(0xB3D50000) // Red
val toastInfoBg = Color(0xB33F51B5) // Indigo Blue
val toastIconCircleBg = Color(0x33FFFFFF) // Semi-transparent White
val toastTextColor = Color.White // White

object ThemeManager {
    private val _themeMode = MutableStateFlow(
        MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        MmkvManager.encodeSettings(AppConfig.PREF_UI_MODE_NIGHT, mode)
        _themeMode.value = mode
    }

    fun refresh() {
        _themeMode.value =
            MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
    }
}

@Composable
fun resolveDarkTheme(): Boolean {
    val mode by ThemeManager.themeMode.collectAsState()
    return when (mode) {
        "1" -> false
        "2" -> true
        else -> isSystemInDarkTheme()
    }
}

val LocalDarkTheme = compositionLocalOf { false }

@Composable
fun AppTheme(
    darkTheme: Boolean = resolveDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColor else LightColor
    val snackbarController = rememberAppSnackbarController()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalAppSnackbar provides snackbarController
    ) {
        MaterialTheme(
            colorScheme = colorScheme
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppSnackbarBridge(controller = snackbarController)
                content()
                AppSnackbarHost(hostState = snackbarController.hostState)
            }
        }
    }
}
