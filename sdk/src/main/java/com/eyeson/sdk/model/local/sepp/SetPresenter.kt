package com.eyeson.sdk.model.local.sepp

import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class SetPresenter(
    val on: Boolean,
    val cid: String
) : LocalBaseCommand