package com.eyeson.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyeson.android.ui.theme.EyesonDemoTheme

@Composable
fun EyesonDemoTextField(
    modifier: Modifier = Modifier,
    label: String = "",
    onValueChange: (String) -> Unit,
    value: String = "",
) {
    TextField(
        value = value,
        onValueChange = { onValueChange(it) },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.overline,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.69f)
            )
        },
        textStyle = MaterialTheme.typography.body1.copy(fontSize = 16.sp),
        maxLines = 1,
        shape = MaterialTheme.shapes.small,
        colors = TextFieldDefaults.textFieldColors(
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Preview
@Composable
fun EyesonDemoTextFieldPreview() {
    EyesonDemoTheme {
        Column {
            var email by rememberSaveable {
                mutableStateOf("")
            }
            EyesonDemoTextField(
                modifier = Modifier
                    .padding(24.dp),
                onValueChange = { email = it },
                label = "Email",
                value = email
            )
        }

    }
}