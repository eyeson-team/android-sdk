package com.eyeson.android.ui.components

import android.os.Build
import android.view.RoundedCorner
import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import kotlin.math.max

@Composable
fun Modifier.applyRoundedCornerPadding(
    @FloatRange(from = 0.0, to = 1.0) leftFraction: Float = 1f,
    @FloatRange(from = 0.0, to = 1.0) topFraction: Float = 1f,
    @FloatRange(from = 0.0, to = 1.0) rightFraction: Float = 1f,
    @FloatRange(from = 0.0, to = 1.0) bottomFraction: Float = 1f,
): Modifier {
    val density = LocalDensity.current
    var additionalPadding by remember { mutableStateOf(PaddingValues()) }

    val view = LocalView.current
    return this then Modifier
        .onGloballyPositioned {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val rootWindowInsets = view.rootWindowInsets

                val topLeft = rootWindowInsets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
                val topRight = rootWindowInsets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)
                val bottomLeft =
                    rootWindowInsets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)
                val bottomRight =
                    rootWindowInsets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)

                val leftRadius =
                    max(topLeft?.radius ?: 0, bottomLeft?.radius ?: 0) * leftFraction
                val topRadius =
                    max(topLeft?.radius ?: 0, topRight?.radius ?: 0) * topFraction
                val rightRadius =
                    max(topRight?.radius ?: 0, bottomRight?.radius ?: 0) * rightFraction
                val bottomRadius =
                    max(bottomLeft?.radius ?: 0, bottomRight?.radius ?: 0) * bottomFraction

                with(density) {
                    additionalPadding = PaddingValues(
                        leftRadius.toDp(),
                        topRadius.toDp(),
                        rightRadius.toDp(),
                        bottomRadius.toDp()
                    )
                }

            }
        }
        .padding(additionalPadding)
        .consumeWindowInsets(additionalPadding)
}
