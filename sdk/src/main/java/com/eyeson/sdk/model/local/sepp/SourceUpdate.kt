package com.eyeson.sdk.model.local.sepp

import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class SourceUpdate(
    val sources: List<String>,
    val videSources: List<Int>,
    val desktopStreamingId: Int?,
    val presenter: Int?
) : LocalBaseCommand