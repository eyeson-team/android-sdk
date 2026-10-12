package com.eyeson.sdk.model.local.meeting

import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class MeetingLockedLocal(
    val locked: Boolean
) : LocalBaseCommand