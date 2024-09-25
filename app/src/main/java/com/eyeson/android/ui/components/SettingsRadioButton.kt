package com.eyeson.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eyeson.android.ui.theme.DisabledContentAlpha
import com.eyeson.android.ui.theme.EyesonDemoTheme

@Composable
fun SettingsRadioButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showDivider: Boolean = true,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title, style = MaterialTheme.typography.bodyLarge, color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContentAlpha)
                    }
                )
            }
            RadioButton(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.padding(end = 8.dp),
                selected = selected
            )
        }
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Preview
@Composable
fun SettingsRadioButtonPreview() {

    var selectedA by remember {
        mutableStateOf(false)
    }

    EyesonDemoTheme {
        Surface {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                SettingsRadioButton("Option A", selectedA, { selectedA = !selectedA })
                SettingsRadioButton("Option B", !selectedA, { selectedA = !selectedA })

            }
        }

    }
}

