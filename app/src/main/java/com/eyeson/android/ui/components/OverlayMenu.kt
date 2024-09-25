package com.eyeson.android.ui.components

import androidx.annotation.FloatRange
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.TopEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eyeson.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayMenu(
    visible: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "",
    showDivider: Boolean = false,
    scrimColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f),
    @FloatRange(from = 0.0, to = 1.0) horizontalContentRatio: Float = 1.0f,
    contentShape: Shape = MaterialTheme.shapes.large,
    contentBackgroundColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        Scrim(scrimColor, visible, onClose)

        Column(
            modifier = Modifier
                .fillMaxWidth(horizontalContentRatio)
                .wrapContentWidth(Alignment.CenterHorizontally)
                .align(TopEnd)
        ) {
            if (visible) {
                Surface {
                    Column {

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .height(TopAppBarDefaults.TopAppBarExpandedHeight)
                                .padding(start = 16.dp)
                        ) {
                            Text(
                                modifier = Modifier.weight(1.0f),
                                text = title,
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = onClose) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    stringResource(id = R.string.close_menu),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (showDivider) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        }
                    }
                }
            }
            AnimatedVisibility(visible = visible, modifier) {
                Surface(shape = contentShape, color = contentBackgroundColor) {
                    content()
                }
            }
        }
    }
}


