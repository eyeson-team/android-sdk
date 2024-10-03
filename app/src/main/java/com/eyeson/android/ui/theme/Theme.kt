package com.eyeson.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


private val LightColorPalette = lightColorScheme(
    primary = DarkGray900,
    secondary = DarkGray900,
    surface = Color.White,
    onSurface = Neutral7,
    background = Color.White,
    inverseSurface = Neutral7,
    inverseOnSurface = Color.White,
    surfaceContainerHighest = Gray400,
    outlineVariant = DarkGray900.copy(alpha = 0.1f)
)

@Composable
fun EyesonDemoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorPalette,
        shapes = Shapes,
        typography = Typography,
        content = content
    )
}