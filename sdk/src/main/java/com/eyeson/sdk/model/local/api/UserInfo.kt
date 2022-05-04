package com.eyeson.sdk.model.local.api

import java.util.*

data class UserInfo(
    val id: String,
    val name: String,
    val avatar: String?,
    val guest: Boolean,
    val joinedAt: Date
)
