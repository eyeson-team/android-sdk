package com.eyeson.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.eyeson.android.ui.theme.Gray500

@Composable
fun SettingsToggle(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String = "",
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
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(),
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.69f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContentAlpha)
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Switch(
                checked = value,
                onCheckedChange = onValueChange,
                enabled = enabled,
                modifier = Modifier.padding(start = 8.dp, end = 16.dp),
            )
        }
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Preview
@Composable
fun SettingsTogglePreview() {
    var on by remember { mutableStateOf(false) }

    EyesonDemoTheme {
        Surface {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                SettingsToggle(on, { on = it }, "Title 42")
                SettingsToggle(
                    on,
                    { on = it },
                    title = "I'm a really long text and should be multi line",
                    description = "I'm the description"
                )
            }
        }

    }
}
