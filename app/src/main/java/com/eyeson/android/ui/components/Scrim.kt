package com.eyeson.android.ui.components

import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import com.eyeson.android.R

/**
 * Based on [ModalBottomSheetState] Scrim
 */
@Composable
fun Scrim(
    color: Color,
    visible: Boolean,
    onClose: () -> Unit,
    closeOnClick: Boolean = true,
) {
    if (color.isSpecified) {
        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = TweenSpec(),
        )

        val closeMenu = stringResource(id = R.string.close_menu)
        val closeModifier = if (visible && closeOnClick) {
            Modifier
                .pointerInput(onClose) { detectTapGestures { onClose() } }
                .semantics(mergeDescendants = true) {
                    contentDescription = closeMenu
                    onClick { onClose(); true }
                }
        } else {
            Modifier
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .then(closeModifier)
        ) {
            drawRect(color = color, alpha = alpha)
        }
    }
}
