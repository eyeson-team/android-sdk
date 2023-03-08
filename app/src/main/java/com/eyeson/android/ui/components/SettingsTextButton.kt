package com.eyeson.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
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

@Composable
fun SettingsTextButton(
    title: String,
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
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
            }
            TextButton(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.padding(end = 8.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colors.onBackground
                )
            ) {
                Text(text = buttonText.uppercase())
            }
        }
        if (showDivider) {
            Divider(modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Preview
@Composable
fun SettingsTextButtonPreview() {

    EyesonDemoTheme {
        Surface {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                SettingsTextButton("Show log", "show", {})
                SettingsTextButton("Audio settings", "change", {})

            }
        }

    }
}

