package com.eyeson.sdk.model.local.api

import java.util.Date

public data class Recording(
    val id: String,
    val duration: Int?,
    val downloadLink: String?,
    val createdAt: Date,
    val user: UserInfo
)
