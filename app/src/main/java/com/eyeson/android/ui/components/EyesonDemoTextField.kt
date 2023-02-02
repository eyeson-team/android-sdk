package com.eyeson.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyeson.android.ui.theme.EyesonDemoTheme

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EyesonDemoTextField(
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    value: String = "",
) {
    val keyboardController = LocalSoftwareKeyboardController.current
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
        keyboardOptions =
        KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = {
            keyboardController?.hide()
        }),
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