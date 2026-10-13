package com.eyeson.sdk.model.api


import com.eyeson.sdk.model.local.api.MeetingOptions
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MeetingOptionsDto(
    @SerialName("background_color")
    val backgroundColor: String,
    @SerialName("broadcast_available")
    val broadcastAvailable: Boolean,
    @SerialName("custom_fields")
    val customFields: Map<String, String>?,
    @SerialName("exit_url")
    val exitUrl: String?,
    @SerialName("kick_available")
    val kickAvailable: Boolean,
    @SerialName("layout")
    val layout: String,
    @SerialName("layout_available")
    val layoutAvailable: Boolean,
    @SerialName("layout_map")
    val layoutMap: List<List<Int>>?,
    @SerialName("layout_name")
    val layoutName: String?,
    @SerialName("layout_users")
    val layoutUsers: List<String>?,
    @SerialName("lock_available")
    val lockAvailable: Boolean,
    @SerialName("reaction_available")
    val reactionAvailable: Boolean,
    @SerialName("recording_available")
    val recordingAvailable: Boolean,
    @SerialName("sfu_mode")
    val sfuMode: String,
    @SerialName("show_label")
    val showLabel: Boolean,
    @SerialName("show_names")
    val showNames: Boolean,
    @SerialName("suggest_guest_names")
    val suggestGuestNames: Boolean,
    @SerialName("voice_activation")
    val voiceActivation: Boolean,
    @SerialName("widescreen")
    val widescreen: Boolean,
) {
    fun toLocal() = MeetingOptions(
        backgroundColor = backgroundColor,
        broadcastAvailable = broadcastAvailable,
        customFields = customFields?: emptyMap(),
        exitUrl = exitUrl,
        kickAvailable = kickAvailable,
        layout = layout,
        layoutAvailable = layoutAvailable,
        layoutMap = layoutMap?.filter {
            it.size == 4
        }?.map {
            MeetingOptions.LayoutMapEntry(
                x = it[0],
                y = it[1],
                width = it[2],
                height = it[3]
            )
        } ?: emptyList(),
        layoutName = layoutName,
        layoutUsers = layoutUsers ?: emptyList(),
        lockAvailable = lockAvailable,
        reactionAvailable = reactionAvailable,
        recordingAvailable = recordingAvailable,
        sfuMode = sfuMode,
        showLabel = showLabel,
        showNames = showNames,
        suggestGuestNames = suggestGuestNames,
        voiceActivation = voiceActivation,
        widescreen = widescreen
    )
}

