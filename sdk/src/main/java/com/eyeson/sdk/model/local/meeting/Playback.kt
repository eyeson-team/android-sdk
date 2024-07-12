package com.eyeson.sdk.model.local.meeting

import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.base.LocalBaseCommand

data class Playback(
    val url: String,
    val name: String?,
    val playId: String?,
    val replacedUser: UserInfo?,
    val audio: Boolean,
    val loopCount: Int
) : LocalBaseCommand
