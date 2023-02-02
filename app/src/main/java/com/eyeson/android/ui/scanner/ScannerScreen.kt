package com.eyeson.android.ui.scanner

import android.net.UrlQuerySanitizer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.eyeson.android.R
import com.eyeson.android.ui.theme.EyesonDemoTheme
import timber.log.Timber

@Composable
fun ScannerScreen(modifier: Modifier = Modifier, onBack: (String) -> Unit = {}) {
    Scaffold(
        modifier = modifier, topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.surface,
                elevation = 0.dp,
                navigationIcon = {
                    IconButton(onClick = {
                        onBack("")
                    }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            stringResource(id = R.string.label_go_back),
                            tint = MaterialTheme.colors.onSurface
                        )
                    }
                },
                title = {
                    Text("")
                }
            )
        }
    ) { padding ->
        val scannerView = rememberCodeScannerViewWithLifecycle(decodeCallback = {
            Timber.d("DecodeCallback: result $it")
            onBack(extractGuestToken(it))
        })

        AndroidView(factory = { scannerView }, modifier = Modifier.padding(padding))
    }
}

fun extractGuestToken(url: String): String {
    val sanitizer = UrlQuerySanitizer().apply {
        allowUnregisteredParamaters = true
        parseUrl(url)
    }
    return sanitizer.getValue("guest") ?: ""
}

@Composable
fun rememberCodeScannerViewWithLifecycle(
    decodeCallback: (String) -> Unit
): CodeScannerView {
    val currentDecodeCallback by rememberUpdatedState(decodeCallback)

    val density = LocalDensity.current
    val scannerViewSizes by remember(density) {
        derivedStateOf {
            ScannerViewSizes(density, 16, 6, 2)
        }
    }
    val context = LocalContext.current
    val scannerView = remember {
        CodeScannerView(context).apply {
            val white = Color.White.toArgb()
            isAutoFocusButtonVisible = false
            flashButtonColor = white
            isFlashButtonVisible = false
            frameColor = white
            frameCornersSize = scannerViewSizes.frameCornersSize
            frameCornersRadius = scannerViewSizes.frameCornersRadius
            frameSize = 0.75f
            frameThickness = scannerViewSizes.frameThickness
            maskColor = Color(0x77000000).toArgb()
        }
    }

    val codeScanner by remember {
        mutableStateOf(CodeScanner(context, scannerView).apply {
            this.decodeCallback = DecodeCallback {
                currentDecodeCallback(it.text)
            }
        })
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(key1 = lifecycle, key2 = scannerView) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                codeScanner.startPreview()
                Timber.d("DecodeCallback: Lifecycle.Event.ON_RESUME")
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                codeScanner.releaseResources()
                Timber.d("DecodeCallback: Lifecycle.Event.ON_PAUSE")
            }
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
    return scannerView
}

class ScannerViewSizes(
    density: Density,
    frameCornersSizeDp: Int,
    frameCornersRadiusDp: Int,
    frameThicknessDp: Int
) {
    val frameCornersSize: Int
    val frameThickness: Int
    val frameCornersRadius: Int

    init {
        with(density) {
            frameCornersSize = frameCornersSizeDp.dp.toPx().toInt()
            frameThickness = frameThicknessDp.dp.toPx().toInt()
            frameCornersRadius = frameCornersRadiusDp.dp.toPx().toInt()
        }
    }
}


@Preview(name = "ScannerScreen")
@Composable
fun ScannerScreenPreview() {
    EyesonDemoTheme {
        ScannerScreen()
    }
}