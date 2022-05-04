package com.eyeson.sdk.model.sepp.base

import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal interface SeppBaseCommandDto {
    val type: String

    fun toLocal(): LocalBaseCommand
}