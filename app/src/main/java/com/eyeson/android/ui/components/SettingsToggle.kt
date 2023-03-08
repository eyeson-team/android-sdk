package com.eyeson.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    showDivider: Boolean = true
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
                    text = title, style = MaterialTheme.typography.body1, color = if (enabled) {
                        MaterialTheme.colors.onSurface
                    } else {
                        MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
                    }
                )
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.caption.copy(),
                        color = if (enabled) {
                            MaterialTheme.colors.onSurface.copy(alpha = 0.69f)
                        } else {
                            MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
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
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colors.primary,
                    checkedTrackColor = MaterialTheme.colors.primary,
                    uncheckedThumbColor = Gray500,
                    uncheckedTrackColor = Gray500,
                )
            )
        }
        if (showDivider) {
            Divider(modifier = Modifier.padding(top = 4.dp))
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

