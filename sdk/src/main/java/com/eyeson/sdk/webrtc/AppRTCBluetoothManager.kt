/*
 *  Copyright 2016 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 *
 * LICENSE: https://chromium.googlesource.com/external/webrtc/+/master/LICENSE
 * PATENTS: https://chromium.googlesource.com/external/webrtc/+/master/PATENTS
 * AUTHORS: https://chromium.googlesource.com/external/webrtc/+/master/AUTHORS
 */

package com.eyeson.sdk.webrtc
//
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED
import android.bluetooth.BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED
import android.bluetooth.BluetoothHeadset.EXTRA_STATE
import android.bluetooth.BluetoothHeadset.STATE_AUDIO_CONNECTED
import android.bluetooth.BluetoothHeadset.STATE_AUDIO_DISCONNECTED
import android.bluetooth.BluetoothHeadset.STATE_DISCONNECTED
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import com.eyeson.sdk.EyesonAudioManager
import com.eyeson.sdk.utils.Logger

/**
 * AppRTCProximitySensor manages functions related to Bluetooth devices in the
 * AppRTC demo.
 */
internal class AppRTCBluetoothManager constructor(
    private val apprtcContext: Context,
    private val apprtcAudioManager: EyesonAudioManager
) {
    // Bluetooth connection state.
    enum class State {
        // Bluetooth is not available; no adapter or Bluetooth is off.
        UNINITIALIZED,  // Bluetooth error happened when trying to start Bluetooth.
        ERROR,  // Bluetooth proxy object for the Headset profile exists, but no connected headset devices,

        // SCO is not started or disconnected.
        HEADSET_UNAVAILABLE,  // Bluetooth proxy object for the Headset profile connected, connected Bluetooth headset

        // present, but SCO is not started or disconnected.
        HEADSET_AVAILABLE,  // Bluetooth audio SCO connection with remote device is closing.
        SCO_DISCONNECTING,  // Bluetooth audio SCO connection with remote device is initiated.
        SCO_CONNECTING,  // Bluetooth audio SCO connection with remote device is established.
        SCO_CONNECTED
    }

    private val audioManager: AudioManager = getAudioManager(apprtcContext)
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var scoConnectionAttempts = 0
    private var bluetoothState: State = State.UNINITIALIZED
    private val bluetoothServiceListener: BluetoothProfile.ServiceListener =
        BluetoothServiceListener()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothHeadset: BluetoothHeadset? = null
    private var bluetoothDevice: BluetoothDevice? = null
    private val bluetoothHeadsetReceiver: BroadcastReceiver = BluetoothHeadsetBroadcastReceiver()


    // Runs when the Bluetooth timeout expires. We use that timeout after calling
    // startScoAudio() or stopScoAudio() because we're not guaranteed to get a
    // callback after those calls.
    private val bluetoothTimeoutRunnable = Runnable { bluetoothTimeout() }

    /**
     * Implementation of an interface that notifies BluetoothProfile IPC clients when they have been
     * connected to or disconnected from the service.
     */
    private inner class BluetoothServiceListener : BluetoothProfile.ServiceListener {
        // Called to notify the client when the proxy object has been connected to the service.
        // Once we have the profile proxy object, we can use it to monitor the state of the
        // connection and perform other operations that are relevant to the headset profile.
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HEADSET || bluetoothState == State.UNINITIALIZED) {
                return
            }
            Logger.d("BluetoothServiceListener.onServiceConnected: BT state=$bluetoothState")

            // Android only supports one connected Bluetooth Headset at a time.
            bluetoothHeadset = proxy as BluetoothHeadset
            updateAudioDeviceState()
            Logger.d("onServiceConnected done: BT state=$bluetoothState")
        }

        /** Notifies the client when the proxy object has been disconnected from the service.  */
        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HEADSET || bluetoothState == State.UNINITIALIZED) {
                return
            }
            Logger.d("BluetoothServiceListener.onServiceDisconnected: BT state=$bluetoothState")
            stopScoAudio()
            bluetoothHeadset = null
            bluetoothDevice = null
            bluetoothState = State.HEADSET_UNAVAILABLE
            updateAudioDeviceState()
            Logger.d("onServiceDisconnected done: BT state=$bluetoothState")
        }
    }

    // Intent broadcast receiver which handles changes in Bluetooth device availability.
    // Detects headset changes and Bluetooth SCO state changes.
    private inner class BluetoothHeadsetBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Logger.d(
                "intent.action ${intent.action}  ${intent.action == ACTION_AUDIO_STATE_CHANGED}     ${
                    intent.getIntExtra(
                        EXTRA_STATE,
                        STATE_DISCONNECTED
                    )
                }"
            )
            if ((bluetoothState == State.UNINITIALIZED) ||
                (intent.action != ACTION_CONNECTION_STATE_CHANGED && intent.action != ACTION_AUDIO_STATE_CHANGED)
            ) {
                return
            }

            when (intent.getIntExtra(EXTRA_STATE, STATE_DISCONNECTED)) {
                BluetoothHeadset.STATE_CONNECTED -> {
                    scoConnectionAttempts = 0
                    updateAudioDeviceState()
                }
                STATE_DISCONNECTED -> {
                    // Bluetooth is probably powered off during the call.
                    stopScoAudio()
                    updateAudioDeviceState()
                }
                STATE_AUDIO_CONNECTED -> {
                    cancelTimer()
                    if (bluetoothState == State.SCO_CONNECTING) {
                        Logger.d("+++ Bluetooth audio SCO is now connected")
                        bluetoothState = State.SCO_CONNECTED
                        scoConnectionAttempts = 0
                        updateAudioDeviceState()
                    } else {
                        Logger.w("Unexpected state BluetoothHeadset.STATE_AUDIO_CONNECTED")
                    }
                }
                STATE_AUDIO_DISCONNECTED -> {
                    Logger.d("+++ Bluetooth audio SCO is now disconnected")
                    if (isInitialStickyBroadcast) {
                        Logger.d("Ignore STATE_AUDIO_DISCONNECTED initial sticky broadcast.")
                        return
                    }
                    updateAudioDeviceState()
                }
            }
        }
    }


    /** Returns the internal state.  */
    val state: State
        get() {
            return bluetoothState
        }

    /**
     * Activates components required to detect Bluetooth devices and to enable
     * BT SCO (audio is routed via BT SCO) for the headset profile. The end
     * state will be HEADSET_UNAVAILABLE but a state machine has started which
     * will start a state change sequence where the final outcome depends on
     * if/when the BT headset is enabled.
     * Example of state change sequence when start() is called while BT device
     * is connected and enabled:
     * UNINITIALIZED --> HEADSET_UNAVAILABLE --> HEADSET_AVAILABLE -->
     * SCO_CONNECTING --> SCO_CONNECTED <==> audio is now routed via BT SCO.
     * Note that the AppRTCAudioManager is also involved in driving this state
     * change.
     */
    fun start() {
        Logger.d("start")
        if (!checkPermission()) {
            Logger.w("Process (pid=" + Process.myPid() + ") lacks BLUETOOTH permission", true)
            return
        }
        if (bluetoothState != State.UNINITIALIZED) {
            Logger.w("Invalid BT state")
            return
        }
        bluetoothHeadset = null
        bluetoothDevice = null
        scoConnectionAttempts = 0

        // Get a handle to the default local Bluetooth adapter.
        bluetoothAdapter = apprtcContext.getSystemService(BluetoothManager::class.java)?.adapter
        if (bluetoothAdapter == null) {
            Logger.w("Device does not support Bluetooth", true)
            return
        }
        // Ensure that the device supports use of BT SCO audio for off call use cases.
        if (!audioManager.isBluetoothScoAvailableOffCall) {
            Logger.e("Bluetooth SCO audio is not available off call", true)
            return
        }
        logBluetoothAdapterInfo(bluetoothAdapter)
        // Establish a connection to the HEADSET profile (includes both Bluetooth Headset and
        // Hands-Free) proxy object and install a listener.
        if (!getBluetoothProfileProxy(
                apprtcContext,
                bluetoothServiceListener,
                BluetoothProfile.HEADSET
            )
        ) {
            Logger.e("BluetoothAdapter.getProfileProxy(HEADSET) failed", true)
            return
        }
        // Register receivers for BluetoothHeadset change notifications.
        val bluetoothHeadsetFilter = IntentFilter().apply {
            addAction(ACTION_CONNECTION_STATE_CHANGED)
            addAction(ACTION_AUDIO_STATE_CHANGED)
        }
        registerReceiver(bluetoothHeadsetReceiver, bluetoothHeadsetFilter)

        Logger.d("Bluetooth proxy for headset profile has started")
        bluetoothState = State.HEADSET_UNAVAILABLE
        Logger.d("start done: BT state=$bluetoothState")
    }

    /** Stops and closes all components related to Bluetooth audio.  */
    fun stop() {
        Logger.d("stop: BT state=$bluetoothState")
        if (bluetoothAdapter == null) {
            return
        }
        // Stop BT SCO connection with remote device if needed.
        stopScoAudio()
        // Close down remaining BT resources.
        if (bluetoothState == State.UNINITIALIZED) {
            return
        }
        unregisterReceiver(bluetoothHeadsetReceiver)
        cancelTimer()
        if (bluetoothHeadset != null) {
            bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset)
            bluetoothHeadset = null
        }
        bluetoothAdapter = null
        bluetoothDevice = null
        bluetoothState = State.UNINITIALIZED
        Logger.d("stop done: BT state=$bluetoothState")
    }

    /**
     * Starts Bluetooth SCO connection with remote device.
     * Note that the phone application always has the priority on the usage of the SCO connection
     * for telephony. If this method is called while the phone is in call it will be ignored.
     * Similarly, if a call is received or sent while an application is using the SCO connection,
     * the connection will be lost for the application and NOT returned automatically when the call
     * ends. Also note that: up to and including API version JELLY_BEAN_MR1, this method initiates a
     * virtual voice call to the Bluetooth headset. After API version JELLY_BEAN_MR2 only a raw SCO
     * audio connection is established.
     * TODO(henrika): should we add support for virtual voice call to BT headset also for JBMR2 and
     * higher. It might be required to initiates a virtual voice call since many devices do not
     * accept SCO audio without a "call".
     */
    fun startScoAudio(): Boolean {
        Logger.d(
            "startSco: BT state=" + bluetoothState + ", "
                    + "attempts: " + scoConnectionAttempts + ", "
                    + "SCO is on: " + isScoOn
        )
        if (bluetoothState != State.HEADSET_AVAILABLE) {
            Logger.e("BT SCO connection fails - no headset available")
            return false
        }
        // Start BT SCO channel and wait for ACTION_AUDIO_STATE_CHANGED.
        Logger.d("Starting Bluetooth SCO and waits for ACTION_AUDIO_STATE_CHANGED...")
        // The SCO connection establishment can take several seconds, hence we cannot rely on the
        // connection to be available when the method returns but instead register to receive the
        // intent ACTION_SCO_AUDIO_STATE_UPDATED and wait for the state to be SCO_AUDIO_STATE_CONNECTED.
        bluetoothState = State.SCO_CONNECTING
        audioManager.startBluetoothSco()
        audioManager.isBluetoothScoOn = true
        scoConnectionAttempts++
        startTimer()
        Logger.d(
            "startScoAudio done: BT state=" + bluetoothState + ", "
                    + "SCO is on: " + isScoOn
        )
        return true
    }

    /** Stops Bluetooth SCO connection with remote device.  */
    fun stopScoAudio() {
        Logger.d(
            "stopScoAudio: BT state=" + bluetoothState + ", "
                    + "SCO is on: " + isScoOn
        )
        if (bluetoothState != State.SCO_CONNECTING && bluetoothState != State.SCO_CONNECTED) {
            return
        }
        cancelTimer()
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        bluetoothState = State.SCO_DISCONNECTING
        Logger.d(
            "stopScoAudio done: BT state=" + bluetoothState + ", "
                    + "SCO is on: " + isScoOn
        )
    }

    /**
     * Use the BluetoothHeadset proxy object (controls the Bluetooth Headset
     * Service via IPC) to update the list of connected devices for the HEADSET
     * profile. The internal state will change to HEADSET_UNAVAILABLE or to
     * HEADSET_AVAILABLE and `bluetoothDevice` will be mapped to the connected
     * device if available.
     */
    fun updateDevice() {
        if (bluetoothState == State.UNINITIALIZED || bluetoothHeadset == null) {
            return
        }
        if (checkPermission()) {
            Logger.d("updateDevice")
            // Get connected devices for the headset profile. Returns the set of
            // devices which are in state STATE_CONNECTED. The BluetoothDevice class
            // is just a thin wrapper for a Bluetooth hardware address.
            bluetoothHeadset?.let { bluetoothHeadset ->
                val devices: List<BluetoothDevice> = bluetoothHeadset.connectedDevices
                if (devices.isEmpty()) {
                    bluetoothDevice = null
                    bluetoothState = State.HEADSET_UNAVAILABLE
                    Logger.d("No connected bluetooth headset")
                } else {
                    // Always use first device in list. Android only supports one device.
                    bluetoothDevice = devices[0]
                    bluetoothState = State.HEADSET_AVAILABLE
                    Logger.d(
                        "Connected bluetooth headset: "
                                + "name=" + bluetoothDevice?.name + ", "
                                + "state=" + stateToString(
                            bluetoothHeadset.getConnectionState(
                                bluetoothDevice
                            )
                        )
                                + ", SCO audio=" + bluetoothHeadset.isAudioConnected(bluetoothDevice)
                    )
                }
                Logger.d("updateDevice done: BT state=$bluetoothState")
            }
        }
    }

    fun deviceAvailableOrConnecting(): Boolean {
        return state == State.SCO_CONNECTED
                || state == State.SCO_CONNECTING
                || state == State.HEADSET_AVAILABLE
    }

    /**
     * Stubs for test mocks.
     */
    protected fun getAudioManager(context: Context): AudioManager {
        return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    protected fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?) {
        apprtcContext.registerReceiver(receiver, filter)
    }

    protected fun unregisterReceiver(receiver: BroadcastReceiver?) {
        apprtcContext.unregisterReceiver(receiver)
    }

    protected fun getBluetoothProfileProxy(
        context: Context?, listener: BluetoothProfile.ServiceListener?, profile: Int
    ): Boolean {
        return bluetoothAdapter?.getProfileProxy(context, listener, profile) ?: false
    }

    /** Logs the state of the local Bluetooth adapter.  */
    @SuppressLint("HardwareIds")
    protected fun logBluetoothAdapterInfo(localAdapter: BluetoothAdapter?) {
        if (checkPermission()) {
            Logger.d(
                "BluetoothAdapter: "
                        + "enabled=" + localAdapter?.isEnabled + ", "
                        + "state=" + stateToString(localAdapter?.state) + ", "
                        + "name=" + localAdapter?.name + ", "
                        + "address=" + localAdapter?.address
            )
            // Log the set of BluetoothDevice objects that are bonded (paired) to the local adapter.
            localAdapter?.bondedDevices?.let { pairedDevices ->
                Logger.d("paired devices:")
                for (device in pairedDevices) {
                    Logger.d(" name=" + device.name + ", address=" + device.address)
                }
            }
        }
    }

    /** Ensures that the audio manager updates its list of available audio devices.  */
    private fun updateAudioDeviceState() {
        Logger.d("updateAudioDeviceState")
        apprtcAudioManager.updateAudioDeviceState()
    }

    /** Starts timer which times out after BLUETOOTH_SCO_TIMEOUT_MS milliseconds.  */
    private fun startTimer() {
        Logger.d("startTimer")
        handler.postDelayed(bluetoothTimeoutRunnable, BLUETOOTH_SCO_TIMEOUT_MS.toLong())
    }

    /** Cancels any outstanding timer tasks.  */
    private fun cancelTimer() {
        Logger.d("cancelTimer")
        handler.removeCallbacks(bluetoothTimeoutRunnable)
    }

    /**
     * Called when start of the BT SCO channel takes too long time. Usually
     * happens when the BT device has been turned on during an ongoing call.
     */
    private fun bluetoothTimeout() {
        if (bluetoothState == State.UNINITIALIZED || bluetoothHeadset == null) {
            return
        }
        if (checkPermission()) {
            Logger.d(
                "bluetoothTimeout: BT state=" + bluetoothState + ", "
                        + "attempts: " + scoConnectionAttempts + ", "
                        + "SCO is on: " + isScoOn
            )
            if (bluetoothState != State.SCO_CONNECTING) {
                return
            }
            // Bluetooth SCO should be connecting; check the latest result.
            var scoConnected = false
            bluetoothHeadset?.let { bluetoothHeadset ->
                val devices = bluetoothHeadset.connectedDevices
                if (devices.isNotEmpty()) {
                    bluetoothDevice = devices[0]
                    if (bluetoothHeadset.isAudioConnected(bluetoothDevice)) {
                        Logger.d("SCO connected with " + bluetoothDevice?.name)
                        scoConnected = true
                    } else {
                        Logger.d("SCO is not connected with " + bluetoothDevice?.name)
                    }
                }
            }
            if (scoConnected) {
                // We thought BT had timed out, but it's actually on; updating state.
                bluetoothState = State.SCO_CONNECTED
                scoConnectionAttempts = 0
            } else {
                // Give up and "cancel" our request by calling stopBluetoothSco().
                Logger.w("BT failed to connect after timeout")
                stopScoAudio()
            }
            updateAudioDeviceState()
            Logger.d("bluetoothTimeout done: BT state=$bluetoothState")
        }
    }

    /** Checks whether audio uses Bluetooth SCO.  */
    private val isScoOn: Boolean
        get() = audioManager.isBluetoothScoOn

    /** Converts BluetoothAdapter states into local string representations.  */
    private fun stateToString(state: Int?): String {
        return when (state) {
            BluetoothAdapter.STATE_DISCONNECTED -> "DISCONNECTED"
            BluetoothAdapter.STATE_CONNECTED -> "CONNECTED"
            BluetoothAdapter.STATE_CONNECTING -> "CONNECTING"
            BluetoothAdapter.STATE_DISCONNECTING -> "DISCONNECTING"
            BluetoothAdapter.STATE_OFF -> "OFF"
            BluetoothAdapter.STATE_ON -> "ON"
            BluetoothAdapter.STATE_TURNING_OFF ->         // Indicates the local Bluetooth adapter is turning off. Local clients should immediately
                // attempt graceful disconnection of any remote links.
                "TURNING_OFF"
            BluetoothAdapter.STATE_TURNING_ON ->         // Indicates the local Bluetooth adapter is turning on. However local clients should wait
                // for STATE_ON before attempting to use the adapter.
                "TURNING_ON"
            else -> "INVALID"
        }
    }

    private fun checkPermission(): Boolean {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            apprtcContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            apprtcContext.checkSelfPermission(Manifest.permission.BLUETOOTH)
        }
        return granted == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        // Timeout interval for starting or stopping audio to a Bluetooth SCO device.
        private const val BLUETOOTH_SCO_TIMEOUT_MS = 4000

        // Maximum number of SCO connection attempts.
        private const val MAX_SCO_CONNECTION_ATTEMPTS = 2
    }
}