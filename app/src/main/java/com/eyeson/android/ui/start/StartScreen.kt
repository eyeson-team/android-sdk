package com.eyeson.android.ui.start

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.windowInsetsEndWidth
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
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyeson.android.R
import com.eyeson.android.ui.components.EyesonDemoTextField
import com.eyeson.android.ui.theme.DisabledContentAlpha
import com.eyeson.android.ui.theme.EyesonDemoTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState


const val PERMALINK_URL = "https://docs.eyeson.com/docs/rest/features/permalink/"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StartRout(
    onScanClicked: () -> Unit = {},
    onSettingsClicked: () -> Unit = {},
    connect: (accessKey: String) -> Unit = { _ -> },
    connectAsGuest: (guestToken: String, guestName: String) -> Unit = { _, _ -> },
    connectPermalink: (userToken: String) -> Unit = { _ -> },
    connectAsGuestPermalink: (guestToken: String, guestName: String) -> Unit = { _, _ -> },
    viewModel: StartViewModel = hiltViewModel(),
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissions = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    val multiplePermissionsState = rememberMultiplePermissionsState(
        permissions
    )

    StartScreen(
        uiState = uiState,
        multiplePermissionsState = multiplePermissionsState,
        onAccessKeyChange = viewModel::setAccessKey,
        onGuestNameChange = viewModel::setGuestName,
        onGuestTokenChange = viewModel::setGuestToken,
        onUserTokenPermalinkChange = viewModel::setUserTokenPermalink,
        onGuestNamePermalinkChange = viewModel::setGuestNamePermalink,
        onGuestTokenPermalinkChange = viewModel::setGuestTokenPermalink,
        onScanClicked = onScanClicked,
        onSettingsClicked = onSettingsClicked,
        connect = { connect(uiState.accessKey) },
        connectAsGuest = { connectAsGuest(uiState.guestToken, uiState.guestName) },
        connectPermalink = { connectPermalink(uiState.userTokenPermalink) },
        connectAsGuestPermalink = {
            connectAsGuestPermalink(
                uiState.guestTokenPermalink,
                uiState.guestNamePermalink
            )
        }
    )
}

@OptIn(
    ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
private fun StartScreen(
    uiState: StartScreenState,
    multiplePermissionsState: MultiplePermissionsState,
    onAccessKeyChange: (accessKey: String) -> Unit,
    onGuestNameChange: (guestName: String) -> Unit,
    onGuestTokenChange: (guestToken: String) -> Unit,
    onUserTokenPermalinkChange: (userTokenPermalink: String) -> Unit,
    onGuestNamePermalinkChange: (guestNamePermalink: String) -> Unit,
    onGuestTokenPermalinkChange: (guestTokenPermalink: String) -> Unit,
    onScanClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    connect: () -> Unit,
    connectAsGuest: () -> Unit,
    connectPermalink: () -> Unit,
    connectAsGuestPermalink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var showPermissionDialog = !multiplePermissionsState.allPermissionsGranted

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier.safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = { /*NOOP*/ },
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

                            uiState.accessKey.isNotBlank() && selectedTab == 0 -> {
                                connect()
                            }

                            uiState.guestName.isNotBlank() && uiState.guestToken.isNotBlank() && selectedTab == 0 -> {
                                connectAsGuest()
                            }

                            uiState.userTokenPermalink.isNotBlank() && selectedTab == 1 -> {
                                connectPermalink()
                            }

                            uiState.guestNamePermalink.isNotBlank() && uiState.guestTokenPermalink.isNotBlank() && selectedTab == 1 -> {
                                connectAsGuestPermalink()
                            }
                        }
                    },
                    contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                    shape = MaterialTheme.shapes.small,
                    enabled = (uiState.canConnect && selectedTab == 0) || (uiState.canConnectPermalink && selectedTab == 1),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.connect).uppercase()
                    )
                }
                Spacer(
                    Modifier
                        .windowInsetsEndWidth(WindowInsets.navigationBarsIgnoringVisibility)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.eyeson_logo_dark),
                contentDescription = stringResource(
                    id = R.string.label_logo
                ),
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = stringResource(id = R.string.android_sdk),
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            val tabColor = @Composable { selected: Boolean ->
                if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContentAlpha)
                }

            }
            SecondaryTabRow(
                modifier = Modifier.padding(top = 24.dp),
                selectedTabIndex = selectedTab
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }) {
                    Text(
                        modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                        text = stringResource(id = R.string.default_tab).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = tabColor(selectedTab == 0)
                    )
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text(
                        modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                        text = stringResource(id = R.string.permalink_tab).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = tabColor(selectedTab == 1)
                    )
                }
            }


            when (selectedTab) {
                0 -> {
                    DefaultConnect(
                        screenState = uiState,
                        onScanClicked = onScanClicked,
                        onAccessKeyChange = onAccessKeyChange,
                        onGuestNameChange = onGuestNameChange,
                        onGuestTokenChange = onGuestTokenChange,
                    )
                }

                1 -> {
                    PermalinkConnect(
                        screenState = uiState,
                        onUserTokenPermalinkChange = onUserTokenPermalinkChange,
                        onGuestNamePermalinkChange = onGuestNamePermalinkChange,
                        onGuestTokenPermalinkChange = onGuestTokenPermalinkChange
                    )
                }
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
private fun DefaultConnect(
    screenState: StartScreenState,
    onScanClicked: () -> Unit,
    onAccessKeyChange: (accessKey: String) -> Unit,
    onGuestNameChange: (guestName: String) -> Unit,
    onGuestTokenChange: (guestToken: String) -> Unit,
    modifier: Modifier = Modifier,
) {

    Column(
        modifier = modifier.padding(start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.enter_access_key),
            modifier = Modifier.padding(top = 40.dp)
        )

        EyesonDemoTextField(
            onValueChange = onAccessKeyChange,
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
            onValueChange = onGuestNameChange,
            label = stringResource(id = R.string.label_guest_name).uppercase(),
            value = screenState.guestName,
            modifier = Modifier
                .padding(top = 16.dp)
        )

        EyesonDemoTextField(
            onValueChange = onGuestTokenChange,
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

@Composable
private fun PermalinkConnect(
    screenState: StartScreenState,
    onUserTokenPermalinkChange: (userTokenPermalink: String) -> Unit,
    onGuestNamePermalinkChange: (guestNamePermalink: String) -> Unit,
    onGuestTokenPermalinkChange: (guestTokenPermalink: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val uriHandler = LocalUriHandler.current
        Text(
            text = buildAnnotatedString {
                append("${stringResource(id = R.string.create_a_new_meeting).trim()} ")
                val link =
                    LinkAnnotation.Url(
                        PERMALINK_URL,
                        TextLinkStyles(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    ) {
                        val url = (it as LinkAnnotation.Url).url
                        uriHandler.openUri(url)
                    }
                withLink(link) { append(stringResource(id = R.string.permalink_user_token)) }
            },
            modifier = Modifier.padding(top = 40.dp),
            textAlign = TextAlign.Center
        )
        EyesonDemoTextField(
            onValueChange = onUserTokenPermalinkChange,
            label = stringResource(id = R.string.label_user_token).uppercase(),
            value = screenState.userTokenPermalink,
            modifier = Modifier
                .padding(top = 16.dp)
        )

        Text(
            text = stringResource(id = R.string.join_via_guest),
            modifier = Modifier.padding(top = 16.dp)
        )
        EyesonDemoTextField(
            onValueChange = onGuestNamePermalinkChange,
            label = stringResource(id = R.string.label_guest_name).uppercase(),
            value = screenState.guestNamePermalink,
            modifier = Modifier
                .padding(top = 16.dp)
        )

        EyesonDemoTextField(
            onValueChange = onGuestTokenPermalinkChange,
            label = stringResource(id = R.string.label_guest_token).uppercase(),
            value = screenState.guestTokenPermalink,
            modifier = Modifier
                .padding(top = 8.dp)
        )
    }
}

@Composable
private fun PermissionsDialog(modifier: Modifier = Modifier) {
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


@OptIn(ExperimentalPermissionsApi::class)
@Preview
@Composable
fun StartScreenPreview() {
    var uiState by remember {
        mutableStateOf(
            StartScreenState(
                accessKey = "",
                guestName = "SDK test user",
                guestToken = "",
                userTokenPermalink = "",
                guestNamePermalink = "SDK test user",
                guestTokenPermalink = ""
            )
        )
    }
    EyesonDemoTheme {
        val multiplePermissionsState = PreviewMultiplePermissionsState(emptyList(), emptyMap())
        StartScreen(
            uiState = uiState,
            multiplePermissionsState = multiplePermissionsState,
            onAccessKeyChange = { uiState = uiState.copy(accessKey = it) },
            onGuestNameChange = { uiState = uiState.copy(guestName = it) },
            onGuestTokenChange = { uiState = uiState.copy(guestToken = it) },
            onUserTokenPermalinkChange = { uiState = uiState.copy(userTokenPermalink = it) },
            onGuestNamePermalinkChange = { uiState = uiState.copy(guestNamePermalink = it) },
            onGuestTokenPermalinkChange = { uiState = uiState.copy(guestTokenPermalink = it) },
            onScanClicked = { /*NOOP*/ },
            onSettingsClicked = { /*NOOP*/ },
            connect = { /*NOOP*/ },
            connectAsGuest = { /*NOOP*/ },
            connectPermalink = { /*NOOP*/ },
            connectAsGuestPermalink = { /*NOOP*/ }
        )
    }
}


// Credit
// https://github.com/google/accompanist/blob/bb1d0e715dec40804bb6fd763eb02746a2895d02/permissions/src/main/java/com/google/accompanist/permissions/MultiplePermissionsState.kt#L118
@OptIn(ExperimentalPermissionsApi::class)
private class PreviewMultiplePermissionsState(
    permissions: List<String>,
    permissionStatuses: Map<String, PermissionStatus>,
) : MultiplePermissionsState {
    override val permissions: List<PermissionState> = permissions.fastMap { permission ->
        PreviewPermissionState(
            permission = permission,
            status = permissionStatuses[permission] ?: PermissionStatus.Granted,
        )
    }

    override val revokedPermissions: List<PermissionState> = emptyList()
    override val allPermissionsGranted: Boolean = true
    override val shouldShowRationale: Boolean = false

    override fun launchMultiplePermissionRequest() {}

    class PreviewPermissionState(
        override val permission: String,
        override val status: PermissionStatus,
    ) : PermissionState {
        override fun launchPermissionRequest() {}
    }
}