package com.eyeson.sdk.model.local.call

import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class StartCallLocal(val meeting: MeetingDto) : LocalBaseCommand