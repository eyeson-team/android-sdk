package com.eyeson.sdk.model.local.api

data class MeetingOptions(
    val backgroundColor: String,
    val broadcastAvailable: Boolean,
    val customFields: Map<String, String>,
    val exitUrl: String?,
    val kickAvailable: Boolean,
    val layout: String,
    val layoutAvailable: Boolean,
    val layoutMap: List<LayoutMapEntry>,
    val layoutName: String?,
    val layoutUsers: List<String>,
    val lockAvailable: Boolean,
    val reactionAvailable: Boolean,
    val recordingAvailable: Boolean,
    val sfuMode: String,
    val showLabel: Boolean,
    val showNames: Boolean,
    val suggestGuestNames: Boolean,
    val voiceActivation: Boolean,
    val widescreen: Boolean,
) {
    data class LayoutMapEntry(val x: Int, val y: Int, val width: Int, val height: Int)
}













