package com.artchknd.tryit.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.artchknd.tryit.R

enum class ThemePreference(@StringRes val labelResId: Int) {
    SYSTEM(R.string.settings_theme_system),
    LIGHT(R.string.settings_theme_light),
    DARK(R.string.settings_theme_dark)
}


@Immutable
data class FairweatherColors(
    val isDark: Boolean,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val iconTint: Color,
    val glassTint: Color,
    val fallbackBackground: Color,
    val errorAccent: Color
)

private val LightColors = FairweatherColors(
    isDark = false,
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF1E293B),
    textMuted = Color(0xFF475569),
    iconTint = Color(0xFF0F172A),
    glassTint = Color.White,
    fallbackBackground = Color(0xFF7CC0F8),
    errorAccent = Color(0xFFB91C1C)
)

private val DarkColors = FairweatherColors(
    isDark = true,
    textPrimary = Color.White,
    textSecondary = Color.White.copy(alpha = 0.82f),
    textMuted = Color.White.copy(alpha = 0.62f),
    iconTint = Color.White,
    glassTint = Color.White,
    fallbackBackground = Color(0xFF0F172A),
    errorAccent = Color(0xFFFCA5A5)
)

private val LocalFairweatherColors = staticCompositionLocalOf { DarkColors }

object FairweatherTheme {
    val colors: FairweatherColors
        @Composable
        get() = LocalFairweatherColors.current
}

@Composable
fun FairweatherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val materialScheme = if (darkTheme) {
        darkColorScheme(
            background = colors.fallbackBackground,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary
        )
    } else {
        lightColorScheme(
            background = colors.fallbackBackground,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalFairweatherColors provides colors) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun TryItTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    FairweatherTheme(darkTheme = darkTheme, content = content)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}