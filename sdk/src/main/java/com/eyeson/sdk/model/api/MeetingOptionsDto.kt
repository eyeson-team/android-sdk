package com.eyeson.sdk.model.api


import com.eyeson.sdk.model.local.api.MeetingOptions
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class MeetingOptionsDto(
    @Json(name = "background_color")
    val backgroundColor: String,
    @Json(name = "broadcast_available")
    val broadcastAvailable: Boolean,
    @Json(name = "custom_fields")
    val customFields: Map<String, String>?,
    @Json(name = "exit_url")
    val exitUrl: String?,
    @Json(name = "kick_available")
    val kickAvailable: Boolean,
    @Json(name = "layout")
    val layout: String,
    @Json(name = "layout_available")
    val layoutAvailable: Boolean,
    @Json(name = "layout_map")
    val layoutMap: List<List<Int>>?,
    @Json(name = "layout_name")
    val layoutName: String?,
    @Json(name = "layout_users")
    val layoutUsers: List<String>?,
    @Json(name = "lock_available")
    val lockAvailable: Boolean,
    @Json(name = "reaction_available")
    val reactionAvailable: Boolean,
    @Json(name = "recording_available")
    val recordingAvailable: Boolean,
    @Json(name = "sfu_mode")
    val sfuMode: String,
    @Json(name = "show_label")
    val showLabel: Boolean,
    @Json(name = "show_names")
    val showNames: Boolean,
    @Json(name = "suggest_guest_names")
    val suggestGuestNames: Boolean,
    @Json(name = "voice_activation")
    val voiceActivation: Boolean,
    @Json(name = "widescreen")
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

