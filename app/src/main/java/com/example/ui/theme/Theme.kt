package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LexoDarkPrimary,
    onPrimary = LexoDarkOnPrimary,
    secondary = LexoDarkSecondary,
    background = LexoDarkBg,
    surface = LexoDarkSurface,
    surfaceVariant = LexoDarkSurfaceVariant,
    outline = LexoDarkOutline,
    onBackground = LexoDarkText,
    onSurface = LexoDarkText,
    onSurfaceVariant = LexoDarkTextMuted
)

private val LightColorScheme = lightColorScheme(
    primary = LexoLightPrimary,
    onPrimary = LexoLightOnPrimary,
    secondary = LexoLightSecondary,
    background = LexoLightBg,
    surface = LexoLightSurface,
    surfaceVariant = LexoLightSurfaceVariant,
    outline = LexoLightOutline,
    onBackground = LexoLightText,
    onSurface = LexoLightText,
    onSurfaceVariant = LexoLightTextMuted
)

@Composable
fun LexoTheme(
    darkTheme: Boolean = true, // Default to dark for a calm, distraction-free environment
    dynamicColor: Boolean = false, // Keep consistent branding colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
