package com.eyeson.android.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorPalette = lightColors(
    primary = DarkGray900,
    secondary = DarkGray900,
    surface = Color.White,
    onSurface = Neutral7
)

@Composable
fun EyesonDemoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = LightColorPalette,
        shapes = Shapes,
        typography = Typography,
        content = content
    )
}