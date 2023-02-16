package com.eyeson.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.eyeson.android.EyesonDemoDestination.GUEST_QR_SCANNER_ROUTE
import com.eyeson.android.EyesonDemoDestination.MEETING_ROUT
import com.eyeson.android.EyesonDemoDestination.START_ROUTE
import com.eyeson.android.EyesonDemoDestination.START_SETTINGS_ROUTE
import com.eyeson.android.EyesonNavigationParameter.ACCESS_KEY
import com.eyeson.android.EyesonNavigationParameter.GUEST_NAME
import com.eyeson.android.EyesonNavigationParameter.GUEST_TOKEN
import com.eyeson.android.ui.meeting.MeetingScreen
import com.eyeson.android.ui.scanner.ScannerScreen
import com.eyeson.android.ui.settings.SettingsScreen
import com.eyeson.android.ui.start.StartScreen
import com.eyeson.android.ui.theme.EyesonDemoTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

object EyesonDemoDestination {
    const val START_ROUTE = "start"
    const val START_SETTINGS_ROUTE = "start_settings"
    const val GUEST_QR_SCANNER_ROUTE = "guest_qr_scanner"
    const val MEETING_ROUT = "meeting"
}

object EyesonNavigationParameter {
    const val ACCESS_KEY = "access_key"
    const val GUEST_TOKEN = "guest_token"
    const val GUEST_NAME = "guest_name"
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun EyesonDemoNavHost(
    multiplePermissionsState: MultiplePermissionsState,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    EyesonDemoTheme {
        val coroutineScope = rememberCoroutineScope()

        NavHost(
            navController = navController,
            startDestination = START_ROUTE,
            modifier = modifier
        ) {

            composable(route = START_ROUTE) {
                StartScreen(
                    multiplePermissionsState = multiplePermissionsState,
                    savedStateHandle = navController.currentBackStackEntry?.savedStateHandle,
                    onScanClicked = { navController.navigateSingleTopTo(GUEST_QR_SCANNER_ROUTE) },
                    onSettingsClicked = { navController.navigateSingleTopTo(START_SETTINGS_ROUTE) },
                    connect = { accessKey -> navController.navigateToMeeting(accessKey) },
                    connectAsGuest = { guestToken, guestName ->
                        navController.navigateToMeeting(
                            guestToken,
                            guestName
                        )
                    }
                )
            }
            composable(route = GUEST_QR_SCANNER_ROUTE) {
                ScannerScreen { guestToken ->
                    coroutineScope.launch(Dispatchers.Main) {
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            GUEST_TOKEN,
                            guestToken
                        )
                        navController.popBackStack()
                    }
                }
            }

            composable(route = START_SETTINGS_ROUTE) {
                SettingsScreen(onBack = {
                    navController.navigateUp()
                })

            }

            composable(
                route = "$MEETING_ROUT?$ACCESS_KEY={$ACCESS_KEY}&$GUEST_TOKEN={$GUEST_TOKEN}&$GUEST_NAME={$GUEST_NAME}",
                arguments = listOf(
                    navArgument(ACCESS_KEY) {
                        nullable = true
                        defaultValue = null
                        type = NavType.StringType

                    },
                    navArgument(GUEST_TOKEN) {
                        nullable = true
                        defaultValue = null
                        type = NavType.StringType

                    },
                    navArgument(GUEST_NAME) {
                        nullable = true
                        defaultValue = null
                        type = NavType.StringType

                    })
            ) {
                MeetingScreen(onBack = {
                    navController.navigateUp()
                })
            }

        }
    }
}

fun NavHostController.navigateSingleTopTo(route: String) {
    Timber.d("route: $route")
    navigate(route) { launchSingleTop = true }
}

fun NavHostController.navigateToMeeting(accessKey: String) =
    navigateSingleTopTo("$MEETING_ROUT?$ACCESS_KEY=$accessKey")

fun NavHostController.navigateToMeeting(guestToken: String, guestName: String) =
    navigateSingleTopTo("$MEETING_ROUT?$GUEST_TOKEN=$guestToken&$GUEST_NAME=$guestName")