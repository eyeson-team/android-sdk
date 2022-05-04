package com.eyeson.sdk.model.local.meeting

import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class RoomReady(val meeting: MeetingDto) : LocalBaseCommand

