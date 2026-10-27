package com.eyeson.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEffect
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.eyeson.android.EyesonDemoDestination.GuestQrScanner
import com.eyeson.android.EyesonDemoDestination.Meeting
import com.eyeson.android.EyesonDemoDestination.Meeting.JoinMode
import com.eyeson.android.EyesonDemoDestination.Start
import com.eyeson.android.EyesonDemoDestination.StartSettings
import com.eyeson.android.ui.meeting.MeetingRout
import com.eyeson.android.ui.meeting.MeetingViewModel
import com.eyeson.android.ui.scanner.ScannerScreen
import com.eyeson.android.ui.settings.SettingsRout
import com.eyeson.android.ui.start.StartRout
import com.eyeson.android.ui.start.StartViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import timber.log.Timber


@Serializable
sealed interface EyesonDemoDestination : NavKey {

    @Serializable
    data object Start : EyesonDemoDestination

    @Serializable
    data object StartSettings : EyesonDemoDestination

    @Serializable
    data object GuestQrScanner : EyesonDemoDestination

    @Serializable
    data class Meeting(val joinMode: JoinMode) : EyesonDemoDestination {

        /**
         * One subtype per way of joining a meeting.
         */
        @Serializable
        sealed interface JoinMode {

            @Serializable
            data class AccessKey(val accessKey: String) : JoinMode

            @Serializable
            data class Guest(val guestToken: String, val guestName: String) : JoinMode

            @Serializable
            data class UserTokenPermalink(val userToken: String) : JoinMode

            @Serializable
            data class GuestPermalink(val guestToken: String, val guestName: String) : JoinMode
        }
    }
}

private const val SCANNED_GUEST_TOKEN = "scanned_guest_token"

@Composable
fun EyesonDemoNavDisplay(
    modifier: Modifier = Modifier,
    backStack: NavBackStack<NavKey> = rememberNavBackStack(Start),
) {

    val coroutineScope = rememberCoroutineScope()

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.goBack() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
            rememberResultEventBusNavEntryDecorator()
        ),
        entryProvider = entryProvider {

            entry<Start> {
                val viewModel: StartViewModel = hiltViewModel()

                ResultEffect<String>(resultKey = SCANNED_GUEST_TOKEN) { guestToken ->
                    viewModel.setGuestToken(guestToken)
                }

                StartRout(
                    onScanClicked = { backStack.navigateSingleTopTo(GuestQrScanner) },
                    onSettingsClicked = { backStack.navigateSingleTopTo(StartSettings) },
                    connect = { accessKey ->
                        backStack.navigateSingleTopTo(Meeting(JoinMode.AccessKey(accessKey)))
                    },
                    connectAsGuest = { guestToken, guestName ->
                        backStack.navigateSingleTopTo(Meeting(JoinMode.Guest(guestToken, guestName)))
                    },
                    connectPermalink = { userToken ->
                        backStack.navigateSingleTopTo(Meeting(JoinMode.UserTokenPermalink(userToken)))
                    },
                    connectAsGuestPermalink = { guestToken, guestName ->
                        backStack.navigateSingleTopTo(
                            Meeting(JoinMode.GuestPermalink(guestToken, guestName))
                        )
                    },
                    viewModel = viewModel
                )
            }

            entry<GuestQrScanner> {
                val resultEventBus = LocalResultEventBus.current

                ScannerScreen { guestToken ->
                    coroutineScope.launch(Dispatchers.Main) {
                        resultEventBus.sendResult(resultKey = SCANNED_GUEST_TOKEN, result = guestToken)
                        backStack.goBack()
                    }
                }
            }

            entry<StartSettings> {
                SettingsRout(onBack = { backStack.goBack() })
            }

            entry<Meeting> { meeting ->
                MeetingRout(
                    onBack = { backStack.goBack() },
                    viewModel = hiltViewModel<MeetingViewModel, MeetingViewModel.Factory>(
                        creationCallback = { factory -> factory.create(meeting.joinMode) }
                    )
                )
            }
        }
    )
}

fun NavBackStack<NavKey>.navigateSingleTopTo(key: NavKey) {
    Timber.d("key: $key")
    if (lastOrNull() != key) {
        add(key)
    }
}

fun NavBackStack<NavKey>.goBack() {
    if (size > 1) {
        removeAt(lastIndex)
    }
}
