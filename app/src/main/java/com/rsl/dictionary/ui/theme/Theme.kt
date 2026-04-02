package com.rsl.dictionary.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimaryBlue,
    primaryContainer = PrimaryBlueContainer,
    onPrimaryContainer = OnPrimaryBlueContainer,
    tertiary = PrimaryBlue,
    onTertiary = OnPrimaryBlue,
    tertiaryContainer = PrimaryBlueContainer,
    onTertiaryContainer = OnPrimaryBlueContainer,
    secondary = SecondaryGray,
    onSecondary = OnSecondaryGray,
    secondaryContainer = SecondaryGrayContainer,
    onSecondaryContainer = OnSecondaryGrayContainer,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceTint = Color.Transparent,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryBlue,
    surfaceDim = SurfaceDimLight,
    surfaceBright = SurfaceBrightLight,
    surfaceContainerLowest = SurfaceLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = ScrimDark,
    error = ErrorRed,
    onError = OnErrorRed
)

@Composable
fun RussianSignLanguageDictionaryAndroidTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography
    ) {
        Surface(color = LightColors.background) {
            content()
        }
    }
}
