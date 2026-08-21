package me.asrielyankare.gachaultils.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Use dynamic colors on Android 12+ if enabled (Material You)
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
        shapes = Shapes,
        content = content
    )
}
