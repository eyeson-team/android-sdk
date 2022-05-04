package com.eyeson.sdk.model.local.meeting

import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class MeetingLocked(
    val locked: Boolean
) : LocalBaseCommand