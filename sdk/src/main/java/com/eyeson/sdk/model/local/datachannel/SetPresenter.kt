package com.eyeson.sdk.model.local.datachannel

import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class SetPresenter(
    val on: Boolean,
    val cid: String
) : LocalBaseCommand