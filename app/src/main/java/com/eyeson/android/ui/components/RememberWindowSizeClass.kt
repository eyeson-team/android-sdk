package com.eyeson.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import androidx.window.layout.WindowMetricsCalculator

@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    val configuration = LocalConfiguration.current

    return remember(configuration) {
        val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity)
        val density = activity.resources.displayMetrics.density
        val widthDp = metrics.bounds.width() / density
        val heightDp = metrics.bounds.height() / density
        WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(widthDp, heightDp)
    }
}
