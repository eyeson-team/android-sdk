package com.eyeson.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eyeson.android.R
import com.eyeson.android.ui.theme.EyesonDemoTheme
import com.eyeson.android.ui.theme.onScrim
import com.eyeson.android.ui.theme.recordingBackground
import com.eyeson.android.ui.theme.recordingIndicator

@Composable
fun RecordingIndicator(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.recordingBackground,
            shape = MaterialTheme.shapes.extraSmall
        )
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            DotIndicator(
                innerColor = MaterialTheme.colorScheme.recordingIndicator,
                innerRadius = 6.dp
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(R.string.recording_abbreviation).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onScrim
            )
        }
    }
}

@Preview
@Composable
private fun RecordingIndicatorPreview() {
    EyesonDemoTheme {
        RecordingIndicator()
    }
}