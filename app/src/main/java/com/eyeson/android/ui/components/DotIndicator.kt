package com.eyeson.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun DotIndicator(
    modifier: Modifier = Modifier,
    innerColor: Color = MaterialTheme.colorScheme.secondary,
    borderColor: Color = MaterialTheme.colorScheme.surface,
    innerRadius: Dp = 5.dp,
    borderWidth: Dp = 0.dp,
) {
    Canvas(
        modifier = modifier.size(innerRadius + borderWidth * 2)
    ) {
        drawCircle(color = innerColor, radius = innerRadius.toPx())

        if (borderWidth > 0.dp) {
            drawCircle(
                color = borderColor,
                radius = (innerRadius).toPx(),
                style = Stroke(width = borderWidth.toPx())
            )
        }
    }
}