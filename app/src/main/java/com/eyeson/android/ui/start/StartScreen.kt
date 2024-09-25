package com.eyeson.android.ui.start

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import com.eyeson.android.EyesonNavigationParameter
import com.eyeson.android.R
import com.eyeson.android.ui.components.EyesonDemoTextField
import com.eyeson.android.ui.theme.EyesonDemoTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(
    multiplePermissionsState: MultiplePermissionsState,
    modifier: Modifier = Modifier,
    savedStateHandle: SavedStateHandle? = null,
    onScanClicked: () -> Unit = {},
    onSettingsClicked: () -> Unit = {},
    connect: (accessKey: String) -> Unit = { _ -> },
    connectAsGuest: (guestToken: String, guestName: String) -> Unit = { _, _ -> },
) {
    val scrollState = rememberScrollState()
    val screenState by rememberSaveable(stateSaver = StartScreenState.Saver) {
        mutableStateOf(StartScreenState())
    }

    var showPermissionDialog = !multiplePermissionsState.allPermissionsGranted

    if (savedStateHandle != null) {
        val guestToken = savedStateHandle.get<String>(EyesonNavigationParameter.GUEST_TOKEN)
        LaunchedEffect(guestToken) {
            guestToken?.let {
                screenState.guestToken = guestToken
                savedStateHandle.remove<String>(EyesonNavigationParameter.GUEST_TOKEN)
            }
        }
    }

    Scaffold(
        modifier = modifier, topBar = {
            TopAppBar(
                title = {                    /*NOOP*/ },
                actions = {
                    IconButton(onClick = { onSettingsClicked() }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            stringResource(id = R.string.label_settings),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(Color.White)
            ) {
                HorizontalDivider()
                Button(
                    onClick = {
                        when {
                            !multiplePermissionsState.allPermissionsGranted -> {
                                showPermissionDialog = true
                            }

                            screenState.accessKey.isNotBlank() -> {
                                connect(screenState.accessKey)
                            }

                            else -> {
                                connectAsGuest(screenState.guestToken, screenState.guestName)
                            }
                        }
                    },
                    contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                    shape = MaterialTheme.shapes.small,
                    enabled = screenState.canConnect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.connect).uppercase()
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(start = 16.dp, end = 16.dp)
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(id = R.drawable.eyeson_logo_dark),
                contentDescription = stringResource(
                    id = R.string.label_logo
                ),
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                text = stringResource(id = R.string.android_sdk),
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = stringResource(id = R.string.enter_access_key),
                modifier = Modifier.padding(top = 40.dp)
            )
            EyesonDemoTextField(
                onValueChange = { screenState.accessKey = it },
                label = stringResource(id = R.string.label_access_key).uppercase(),
                value = screenState.accessKey,
                modifier = Modifier
                    .padding(top = 16.dp)
            )

            Text(
                text = stringResource(id = R.string.join_via_guest),
                modifier = Modifier.padding(top = 16.dp)
            )
            EyesonDemoTextField(
                onValueChange = { screenState.guestName = it },
                label = stringResource(id = R.string.label_guest_name).uppercase(),
                value = screenState.guestName,
                modifier = Modifier
                    .padding(top = 16.dp)
            )

            EyesonDemoTextField(
                onValueChange = { screenState.guestToken = it },
                label = stringResource(id = R.string.label_guest_token).uppercase(),
                value = screenState.guestToken,
                modifier = Modifier
                    .padding(top = 8.dp)
            )
            OutlinedButton(
                onClick = {
                    onScanClicked()
                },
                shape = MaterialTheme.shapes.small,
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.scan_qr).uppercase()
                )
            }
        }
    }

    SideEffect {
        multiplePermissionsState.launchMultiplePermissionRequest()
    }

    if (showPermissionDialog) {
        PermissionsDialog()
    }
}

@Composable
fun PermissionsDialog(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AlertDialog(
        modifier = modifier,
        onDismissRequest = { /** intentionally empty **/ },
        title = {
            Text(
                text = stringResource(id = R.string.permissions),
                style = MaterialTheme.typography.displayLarge
            )
        },
        text = {
            Text(text = stringResource(id = R.string.permissions_description))
        },
        confirmButton = {
            Button(
                onClick = {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:" + context.packageName)
                        context.startActivity(this)
                    }
                },
                modifier = Modifier.padding(bottom = 8.dp, end = 8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.go_to_settings).uppercase()
                )
            }
        }
    )
}

private class StartScreenState {
    // Default
    var accessKey by mutableStateOf("")
    var guestName by mutableStateOf("SDK test user")
    var guestToken by mutableStateOf("")

    // Permalink
    var userTokenPermalink by mutableStateOf("")
    var guestNamePermalink by mutableStateOf("SDK test user")
    var guestTokenPermalink by mutableStateOf("")


    val canConnect: Boolean
        get() = accessKey.isNotBlank() || (guestName.isNotBlank() && guestToken.isNotBlank())

    val canConnectPermalink: Boolean
        get() = userTokenPermalink.isNotBlank() || (guestNamePermalink.isNotBlank() && guestTokenPermalink.isNotBlank())

    companion object {
        val Saver: Saver<StartScreenState, *> = listSaver(
            save = {
                listOf(
                    it.accessKey,
                    it.guestName,
                    it.guestToken,
                    it.userTokenPermalink,
                    it.guestNamePermalink,
                    it.guestTokenPermalink
                )
            },
            restore = {
                StartScreenState().apply {
                    accessKey = it[0]
                    guestName = it[1]
                    guestToken = it[2]
                    userTokenPermalink = it[3]
                    guestNamePermalink = it[4]
                    guestTokenPermalink = it[5]
                }
            }
        )
    }
}


@OptIn(ExperimentalPermissionsApi::class)
@Preview(name = "StartScreen")
@Composable
fun StartScreenPreview() {
    EyesonDemoTheme {
        val multiplePermissionsState = rememberMultiplePermissionsState(emptyList())
        StartScreen(multiplePermissionsState)
    }
}