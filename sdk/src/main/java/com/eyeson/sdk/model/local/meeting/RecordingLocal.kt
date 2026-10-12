package com.eyeson.sdk.model.local.meeting

import com.eyeson.sdk.model.local.api.Recording
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.base.LocalBaseCommand
import java.util.Date

internal data class RecordingLocal(
    val id: String,
    val duration: Int?,
    val downloadLink: String?,
    val createdAt: Date,
    val user: UserInfo
) : LocalBaseCommand

internal fun RecordingLocal.toLocal(): Recording {
    return Recording(
        id = id,
        duration = duration,
        downloadLink = downloadLink,
        createdAt = createdAt,
        user = user
    )
}
