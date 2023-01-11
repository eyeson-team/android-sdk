package com.eyeson.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.eyeson.android.EyesonNavigationParameter.GUEST_TOKEN
import com.eyeson.android.ui.scanner.ScannerScreen
import com.eyeson.android.ui.start.StartScreen
import com.eyeson.android.ui.theme.EyesonDemoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object EyesonDemoDestination {
    const val START_ROUTE = "start"
    const val START_SETTINGS_ROUTE = "start_settings"
    const val GUEST_QR_SCANNER_ROUTE = "guest_qr_scanner"
    const val CALL_ROUT = "call"
}

object EyesonNavigationParameter {
    const val GUEST_TOKEN = "guest_token"
}

@Composable
fun EyesonDemoNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    EyesonDemoTheme {
        val coroutineScope = rememberCoroutineScope()

        NavHost(
            navController = navController,
            startDestination = EyesonDemoDestination.START_ROUTE,
            modifier = modifier
        ) {

            composable(route = EyesonDemoDestination.START_ROUTE) {
                StartScreen(
                    savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
                ) {
                    navController.navigateSingleTopTo(EyesonDemoDestination.GUEST_QR_SCANNER_ROUTE)
                }

            }
            composable(route = EyesonDemoDestination.GUEST_QR_SCANNER_ROUTE) {
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
        }
    }
}

fun NavHostController.navigateSingleTopTo(route: String) =
    navigate(route) { launchSingleTop = true }
