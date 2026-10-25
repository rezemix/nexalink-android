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

// Фирменные цвета NEXALINK — как на сайте nexalink.online: белый фон, тёмно-синяя
// кнопка (primary), оранжевый — только мелкие акценты (secondary), зелёный — «подключено».
private val LightColor = lightColorScheme(
    primary = Color(0xFF0A1F3A), // Navy — главные кнопки
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF5F6F8), // фон кружков/плашек
    onPrimaryContainer = Color(0xFF0A1F3A),
    secondary = Color(0xFFEE451B), // Orange — акцент
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF5F6F8), // выбранная строка
    onSecondaryContainer = Color(0xFF0A1F3A),
    tertiary = Color(0xFF1D9A54), // Green — подключено
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE3F4EA),
    onTertiaryContainer = Color(0xFF0B3A20),
    error = Color(0xFFC2362C),
    errorContainer = Color(0xFFFBECE9),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0A1F3A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A1F3A),
    surfaceVariant = Color(0xFFF5F6F8),
    onSurfaceVariant = Color(0xFF566074),
    outline = Color(0xFFD4D8DF),
    outlineVariant = Color(0xFFE6E8EC),
    inverseSurface = Color(0xFF0A1F3A),
    inverseOnSurface = Color(0xFFF4F6FA),
    inversePrimary = Color(0xFFAEC0D6),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFBFC),
    surfaceContainer = Color(0xFFF5F6F8),
    surfaceContainerHigh = Color(0xFFEFF1F4),
    surfaceContainerHighest = Color(0xFFE9ECF0),
)

private val DarkColor = darkColorScheme(
    primary = Color(0xFFECEFF4), // светлая кнопка на тёмном фоне, как на сайте
    onPrimary = Color(0xFF0F1218),
    primaryContainer = Color(0xFF151922),
    onPrimaryContainer = Color(0xFFECEFF4),
    secondary = Color(0xFFFF6A3D),
    onSecondary = Color(0xFF2B1206),
    secondaryContainer = Color(0xFF1F2530),
    onSecondaryContainer = Color(0xFFECEFF4),
    tertiary = Color(0xFF4CC884),
    onTertiary = Color(0xFF0B2A18),
    tertiaryContainer = Color(0xFF14281D),
    onTertiaryContainer = Color(0xFFBDF0D2),
    error = Color(0xFFEF7A70),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1218),
    onBackground = Color(0xFFECEFF4),
    surface = Color(0xFF0F1218),
    onSurface = Color(0xFFECEFF4),
    surfaceVariant = Color(0xFF181D27),
    onSurfaceVariant = Color(0xFFA2AAB8),
    outline = Color(0xFF323A48),
    outlineVariant = Color(0xFF242A35),
    inverseSurface = Color(0xFFECEFF4),
    inverseOnSurface = Color(0xFF181D27),
    inversePrimary = Color(0xFF0A1F3A),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF0F1218),
    surfaceContainerLowest = Color(0xFF0B0E13),
    surfaceContainerLow = Color(0xFF131720),
    surfaceContainer = Color(0xFF181D27),
    surfaceContainerHigh = Color(0xFF1E242F),
    surfaceContainerHighest = Color(0xFF252C38),
)

// Semantic Colors
val colorPing = Color(0xFF1D9A54) // Green
val colorPingRed = Color(0xFFC2362C) // Red
val colorConfigType = Color(0xFFEE451B) // Orange
val colorFabActive = Color(0xFFEE451B) // Orange
val colorFabInactiveLight = Color(0xFF9C9C9C) // Gray
val colorFabInactiveDark = Color(0xFF646464) // Dark Gray
val dividerColorLight = Color(0xFFE0E0E0) // Light Gray
val dividerColorDark = Color(0xFF424242) // Dark Gray

// Большая кнопка подключения на главном экране.
val colorConnected = Color(0xFF1D9A54)      // Зелёный — подключено (как на сайте)
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
