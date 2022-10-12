/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
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
 *
 * Based on: https://chromium.googlesource.com/external/webrtc/+/refs/heads/master/examples/androidapp/src/org/appspot/apprtc/AppRTCAudioManager.java
 */
package com.eyeson.sdk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Build
import com.eyeson.sdk.EyesonAudioManager.AudioDevice.Bluetooth
import com.eyeson.sdk.EyesonAudioManager.AudioDevice.Earpiece
import com.eyeson.sdk.EyesonAudioManager.AudioDevice.SpeakerPhone
import com.eyeson.sdk.EyesonAudioManager.AudioDevice.WiredHeadset
import com.eyeson.sdk.utils.Logger
import com.eyeson.sdk.webrtc.AppRTCBluetoothManager

class EyesonAudioManager constructor(
    private val apprtcContext: Context,
    devicePriority: List<AudioDevice> = listOf(
        Bluetooth,
        WiredHeadset,
        SpeakerPhone,
        Earpiece
    ),
    private val audioFocusChangeListener: OnAudioFocusChangeListener = OnAudioFocusChangeListener {}
) {
    var devicePriority = devicePriority.distinct()
        set(value) {
            field = value.distinct()
            updateAudioDeviceState()
        }

    /**
     * AudioDevice is the names of possible audio devices that we currently support.
     */
    sealed class AudioDevice {
        object SpeakerPhone : AudioDevice()
        object WiredHeadset : AudioDevice()
        object Earpiece : AudioDevice()
        object Bluetooth : AudioDevice()
        object None : AudioDevice()
    }

    interface AudioManagerEvents {
        // Callback fired once audio device is changed or list of available audio devices changed.
        fun onAudioDeviceChanged(
            selectedAudioDevice: AudioDevice,
            availableAudioDevices: Set<AudioDevice>
        )
    }

    /** AudioManager state.  */
    internal enum class AudioManagerState {
        UNINITIALIZED, RUNNING
    }

    private val audioManager: AudioManager =
        apprtcContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioManagerEvents: AudioManagerEvents? = null
    private var amState: AudioManagerState = AudioManagerState.UNINITIALIZED
    private var audioFocusRequest: AudioFocusRequest? = null

    private var savedAudioMode: Int = 0
    private var savedIsSpeakerPhoneOn = false
    private var savedIsMicrophoneMute = false
    private var hasWiredHeadset = false

    private val bluetoothManager: AppRTCBluetoothManager =
        AppRTCBluetoothManager(apprtcContext, this)

    private var audioDevices: MutableSet<AudioDevice> = HashSet()
    private var selectedAudioDevice: AudioDevice = AudioDevice.None
    private var userSelectedAudioDevice: AudioDevice = AudioDevice.None

    private val wiredHeadsetReceiver: BroadcastReceiver = WiredHeadsetReceiver()

    private inner class WiredHeadsetReceiver : BroadcastReceiver() {
        private val stateUnplugged = 0
        private val statePlugged = 1
        private val hasNoMic = 0
        private val hasMic = 1

        override fun onReceive(context: Context, intent: Intent) {
            val state: Int = intent.getIntExtra("state", stateUnplugged)
            val microphone: Int = intent.getIntExtra("microphone", hasNoMic)
            val name: String = intent.getStringExtra("name") ?: ""
            Logger.d(
                "WiredHeadsetReceiver.onReceive: " +
                        "a=${intent.action.toString()}, " +
                        "s=${(if (state == stateUnplugged) "unplugged" else "plugged")}," +
                        "m=${(if (microphone == hasMic) "mic" else "no mic")}, " +
                        "n=$name, " +
                        "sb=$isInitialStickyBroadcast"
            )
            hasWiredHeadset = state == statePlugged
            updateAudioDeviceState()
        }
    }

    fun start(audioManagerEvents: AudioManagerEvents) {
        Logger.d("start")
        if (amState == AudioManagerState.RUNNING) {
            Logger.e("AudioManager is already active", true)
            return
        }
        this.audioManagerEvents = audioManagerEvents
        amState = AudioManagerState.RUNNING

        savedAudioMode = audioManager.mode
        savedIsSpeakerPhoneOn = audioManager.isSpeakerphoneOn
        savedIsMicrophoneMute = audioManager.isMicrophoneMute
        hasWiredHeadset = hasWiredHeadset()

        requestAudioFocus()
        audioManager.isMicrophoneMute = false

        userSelectedAudioDevice = AudioDevice.None
        selectedAudioDevice = AudioDevice.None
        audioDevices.clear()

        bluetoothManager.start()

        updateAudioDeviceState()

        apprtcContext.registerReceiver(
            wiredHeadsetReceiver,
            IntentFilter(Intent.ACTION_HEADSET_PLUG)
        )
        Logger.d("AudioManager started")
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()

            audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    fun stop() {
        Logger.d("stop")
        if (amState != AudioManagerState.RUNNING) {
            Logger.e("Trying to stop AudioManager in incorrect state: $amState", true)
            return
        }
        amState = AudioManagerState.UNINITIALIZED
        apprtcContext.unregisterReceiver(wiredHeadsetReceiver)
        bluetoothManager.stop()

        // Restore previously stored audio states.
        audioManager.isSpeakerphoneOn = savedIsSpeakerPhoneOn
        audioManager.isMicrophoneMute = savedIsMicrophoneMute
        audioManager.mode = savedAudioMode

        // Abandon audio focus. Gives the previous focus owner, if any, focus.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }

        audioManagerEvents = null
        Logger.d("AudioManager stopped")
    }

    private fun setAudioDeviceInternal(device: AudioDevice) {
        audioManager.isSpeakerphoneOn = when (device) {
            SpeakerPhone -> {
                audioManager.mode = AudioManager.MODE_NORMAL
                true
            }
            Earpiece -> {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                false
            }
            WiredHeadset -> {
                audioManager.mode = AudioManager.MODE_NORMAL
                false
            }
            Bluetooth -> {
                audioManager.mode = AudioManager.MODE_NORMAL
                false
            }
            else -> {
                Logger.e("Invalid audio device selection")
                audioManager.isSpeakerphoneOn
            }
        }
        selectedAudioDevice = device
    }

    /** Changes selection of the currently active audio device.  */
    fun selectAudioDevice(device: AudioDevice) {
        if (!audioDevices.contains(device)) {
            Logger.e("Can not select $device from available $audioDevices", true)
        }
        userSelectedAudioDevice = device
        updateAudioDeviceState()
    }

    fun getAudioDevices(): Set<AudioDevice> {
        return audioDevices
    }

    fun getSelectedAudioDevice(): AudioDevice {
        return selectedAudioDevice
    }

    private fun hasEarpiece(): Boolean {
        return apprtcContext.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    }

    /**
     * Checks whether a wired headset is connected or not.
     * This is not a valid indication that audio playback is actually over
     * the wired headset as audio routing depends on other conditions. We
     * only use it as an early indicator (during initialization) of an attached
     * wired headset.
     */
    private fun hasWiredHeadset(): Boolean {
        val devices: Array<AudioDeviceInfo> =
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        for (device in devices) {
            val type: Int = device.type
            if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                Logger.d("hasWiredHeadset: found wired headset")
                return true
            } else if (type == AudioDeviceInfo.TYPE_USB_DEVICE) {
                Logger.d("hasWiredHeadset: found USB audio device")
                return true
            }
        }
        return false
    }

    internal fun updateAudioDeviceState() {
        if (amState != AudioManagerState.RUNNING) {
            Logger.e(
                " ${this::class.java.name} is not in state: ${AudioManagerState.RUNNING.name}",
                true
            )
            return
        }

        if (bluetoothManager.state == AppRTCBluetoothManager.State.HEADSET_AVAILABLE
            || bluetoothManager.state == AppRTCBluetoothManager.State.HEADSET_UNAVAILABLE
            || bluetoothManager.state == AppRTCBluetoothManager.State.SCO_DISCONNECTING
        ) {
            bluetoothManager.updateDevice()
        }


        val newAudioDevices = mutableListOf<AudioDevice>(SpeakerPhone)
        devicePriority.reversed().forEach { audioDevice ->
            when (audioDevice) {
                Bluetooth -> {
                    if (bluetoothManager.deviceAvailableOrConnecting()) {
                        newAudioDevices.add(0, Bluetooth)
                    }
                }
                WiredHeadset -> {
                    if (hasWiredHeadset) {
                        newAudioDevices.add(0, WiredHeadset)
                    }
                }
                Earpiece -> {
                    if (hasEarpiece() && !hasWiredHeadset) {
                        newAudioDevices.add(0, Earpiece)
                    }
                }
                else -> {
                    newAudioDevices.add(0, SpeakerPhone)
                }
            }
        }

        var audioDeviceSetUpdated = audioDevices != newAudioDevices.toMutableSet()
        audioDevices = newAudioDevices.toMutableSet()


        correctUserSelectedAudioDevice()

        var newAudioDevice: AudioDevice = if (userSelectedAudioDevice != AudioDevice.None) {
            userSelectedAudioDevice
        } else {
            newAudioDevices[0]
        }

        val needBluetoothAudioStart =
            (bluetoothManager.state == AppRTCBluetoothManager.State.HEADSET_AVAILABLE
                    && (newAudioDevice == Bluetooth))

        val needBluetoothAudioStop =
            ((bluetoothManager.state == AppRTCBluetoothManager.State.SCO_CONNECTED || bluetoothManager.state == AppRTCBluetoothManager.State.SCO_CONNECTING)
                    && (newAudioDevice != Bluetooth))

        when {
            needBluetoothAudioStop -> {
                bluetoothManager.stopScoAudio()
                bluetoothManager.updateDevice()
            }
            needBluetoothAudioStart -> {
                // Attempt to start Bluetooth SCO audio (takes a few second to start).
                if (!bluetoothManager.startScoAudio()) {
                    // Remove BLUETOOTH from list of available devices since SCO failed.
                    audioDevices.remove(Bluetooth)
                    newAudioDevice = newAudioDevices.getOrNull(1) ?: SpeakerPhone
                    audioDeviceSetUpdated = true
                }
            }
        }

        if (newAudioDevice != AudioDevice.None
            && (newAudioDevice != selectedAudioDevice || audioDeviceSetUpdated)
        ) {
            setAudioDeviceInternal(newAudioDevice)
            audioManagerEvents?.onAudioDeviceChanged(selectedAudioDevice, audioDevices)
        }
    }

    private fun correctUserSelectedAudioDevice() {
        if (bluetoothManager.state == AppRTCBluetoothManager.State.HEADSET_UNAVAILABLE
            && userSelectedAudioDevice == Bluetooth
        ) {
            userSelectedAudioDevice = AudioDevice.None
        }
        if (hasWiredHeadset && userSelectedAudioDevice == Earpiece) {
            userSelectedAudioDevice = WiredHeadset
        }
        if (!hasWiredHeadset && userSelectedAudioDevice == WiredHeadset) {
            userSelectedAudioDevice = SpeakerPhone
        }
    }
}
