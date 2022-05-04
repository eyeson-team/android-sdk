package com.eyeson.sdk.model.local.meeting

import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.base.LocalBaseCommand
import java.util.*

data class Recording(
    val id: String,
    val duration: Int?,
    val downloadLink: String?,
    val createdAt: Date,
    val user: UserInfo
) : LocalBaseCommand

