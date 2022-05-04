package com.eyeson.sdk.model.meeting.base

import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal interface MeetingBaseMessageDto {
    val type: String?

    fun toLocal(): LocalBaseCommand
}