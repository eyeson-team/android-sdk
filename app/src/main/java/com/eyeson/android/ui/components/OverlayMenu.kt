package com.eyeson.android.ui.components

import androidx.annotation.FloatRange
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.contentColorFor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.TopEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.eyeson.android.R

@Composable
fun OverlayMenu(
    visible: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "",
    showDivider: Boolean = false,
    scrimColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.40f),
    @FloatRange(from = 0.0, to = 1.0) horizontalContentRatio: Float = 1.0f,
    contentShape: Shape = MaterialTheme.shapes.large,
    contentBackgroundColor: Color = MaterialTheme.colors.surface,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier) {
        Scrim(scrimColor, visible, onClose)

        Column(
            modifier = Modifier
                .fillMaxWidth(horizontalContentRatio)
                .wrapContentWidth(Alignment.CenterHorizontally)
                .align(TopEnd)
        ) {
            if (visible) {
                TopAppBar(
                    backgroundColor = contentBackgroundColor,
                    elevation = 0.dp,
                    title = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.h1
                        )
                    },
                    actions = {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                stringResource(id = R.string.close_menu),
                                tint = contentColorFor(contentBackgroundColor)
                            )
                        }
                    }
                )
                if (showDivider) {
                    Divider(Modifier.background(color = contentBackgroundColor))
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


