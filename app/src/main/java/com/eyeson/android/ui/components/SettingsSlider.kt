package com.eyeson.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eyeson.android.ui.theme.DisabledContentAlpha
import com.eyeson.android.ui.theme.EyesonDemoTheme
import kotlin.math.roundToInt

/**
 * Picks one of [valueCount] values by its index. The value itself is not known here; [valueLabel]
 * is what the caller wants to show for the currently selected [index].
 */
@Composable
fun SettingsSlider(
    title: String,
    valueLabel: String,
    index: Int,
    valueCount: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showDivider: Boolean = true,
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContentAlpha)
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Slider(
            value = index.toFloat(),
            // The slider only ever hands out whole steps, but rounding keeps a float that is a
            // hair below its step from truncating to the previous index.
            onValueChange = { onIndexChange(it.roundToInt().coerceIn(0, valueCount - 1)) },
            valueRange = 0f..(valueCount - 1).coerceAtLeast(1).toFloat(),
            // steps counts the values between the two ends, not the selectable ones.
            steps = (valueCount - 2).coerceAtLeast(0),
            enabled = enabled,
            colors = SliderDefaults.colors().copy(activeTickColor = MaterialTheme.colorScheme.onPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp)
        )
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Preview
@Composable
fun SettingsSliderPreview() {
    val fps = listOf(5, 10, 15, 20, 25, 30)
    var fpsIndex by remember { mutableIntStateOf(2) }

    EyesonDemoTheme {
        Surface {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                SettingsSlider(
                    title = "Frames per second",
                    valueLabel = fps[fpsIndex].toString(),
                    index = fpsIndex,
                    valueCount = fps.size,
                    onIndexChange = { fpsIndex = it }
                )
            }
        }
    }
}
