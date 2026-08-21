package me.asrielyankare.gachaultils.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    onPrimary = SurfaceCream,
    primaryContainer = PrimaryOrangeLight,
    onPrimaryContainer = DarkBrown,
    secondary = MutedBrown,
    onSecondary = SurfaceCream,
    secondaryContainer = LightBrown,
    onSecondaryContainer = DarkBrown,
    tertiary = SuccessGreen,
    onTertiary = SurfaceCream,
    background = WarmCream,
    onBackground = DarkBrown,
    surface = SurfaceCream,
    onSurface = DarkBrown,
    surfaceVariant = CardBorder,
    onSurfaceVariant = MediumBrown,
    outline = CardBorder,
    outlineVariant = Divider
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryOrangeLight,
    onPrimary = DarkBrown,
    primaryContainer = PrimaryOrangeDark,
    onPrimaryContainer = SurfaceCream,
    secondary = DarkTextSecondary,
    onSecondary = DarkBackground,
    secondaryContainer = DarkCard,
    onSecondaryContainer = DarkText,
    tertiary = SuccessGreen,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkCard,
    outlineVariant = DarkTextSecondary
)

@Composable
fun GachaUltilsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
