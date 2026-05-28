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

private val DarkColorScheme =
  darkColorScheme(
    primary = SportWork,
    secondary = SportPrepare,
    tertiary = SportRest,
    background = SportDarkBg,
    surface = SportCardBg,
    onPrimary = SportDarkBg,
    onSecondary = SportDarkBg,
    onTertiary = SportWhite,
    onBackground = SportWhite,
    onSurface = SportWhite,
    error = SportAccent
  )

private val LightColorScheme = DarkColorScheme // Always force clean dark mode UI as requested by user!

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force clean dark mode UI for maximum athletic high-contrast readability
  dynamicColor: Boolean = false, // Disable dynamic colors so our intentional athletic color branding is preserved
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
