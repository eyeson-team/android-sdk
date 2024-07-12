package com.eyeson.sdk.model.meeting.incoming


internal enum class MeetingMessagesIncoming(val type: String) {
    ROOM_SETUP("room_setup"),
    ROOM_READY("room_ready"),
    CHAT("chat"),
    SNAPSHOT_UPDATE("snapshot_update"),
    PLAYBACK_UPDATE("playback_update"),
    RECORDING_UPDATE("recording_update"),
    BROADCASTS_UPDATE("broadcasts_update"),
    MUTE_LOCAL_AUDIO("stfu"),
    ROOM_LOCKED("lock"),
    CUSTOM("custom"),
    PRESENTATION_UPDATE("presentation_update")
}

