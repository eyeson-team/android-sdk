package com.eyeson.sdk.model.local.api

internal data class UserWithSignaling(
    val user: UserInfo,
    val signalingId: String
)
