package com.eyeson.android.ui.components

import androidx.annotation.FloatRange
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsEndWidth
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.TopEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.eyeson.android.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    insertEdgeToEdgePadding: Boolean,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        var composableSize by remember { mutableStateOf(Size.Zero) }

        Scrim(scrimColor, visible, onClose)
        Column(modifier = Modifier.align(TopEnd)) {
            Row {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(horizontalContentRatio)
                        .wrapContentWidth(Alignment.CenterHorizontally)

                ) {
                    if (visible) {
                        if (insertEdgeToEdgePadding) {
                            Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .windowInsetsTopHeight(WindowInsets.displayCutout)
                            )
                        }
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
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.1f
                                        )
                                    )
                                }
                            }
                        }
                    }
                    AnimatedVisibility(visible = visible, modifier) {
                        Surface(
                            shape = contentShape,
                            color = contentBackgroundColor,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                // Capture the size of the Composable
                                composableSize = coordinates.size.toSize()
                            }) {
                            content()
                        }
                    }
                }
                val density = LocalDensity.current
                val heightInDp =
                    with(density) { composableSize.height.toDp() + TopAppBarDefaults.TopAppBarExpandedHeight + DividerDefaults.Thickness }
                AnimatedVisibility(visible = visible, modifier) {
                    if (insertEdgeToEdgePadding && visible) {
                        val landscapeNavigationBarPadding =
                            WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues()
                        val layoutDirection = LocalLayoutDirection.current
                        Spacer(
                            Modifier
                                .height(heightInDp)
                                .background(contentBackgroundColor)
                                .windowInsetsEndWidth(WindowInsets.displayCutout)
                        )
                        Spacer(
                            Modifier
                                .height(heightInDp)
                                .background(contentBackgroundColor)
                                .windowInsetsEndWidth(WindowInsets.navigationBarsIgnoringVisibility)
                        )

                        Spacer(
                            modifier = Modifier
                                .height(heightInDp)
                                .background(contentBackgroundColor)
                                .size(
                                    if (landscapeNavigationBarPadding.calculateEndPadding(
                                            layoutDirection
                                        ) != 0.dp
                                    ) {
                                        0.dp
                                    } else {
                                        16.dp
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}


