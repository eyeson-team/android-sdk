package com.eyeson.sdk.model.local.sepp

import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class MemberListUpdate(
    val added: List<String>,
    val deleted: List<String>,
    val memberCountAfterUpdate: Int,
    val mediaPlayIds:List<String>
) : LocalBaseCommand
