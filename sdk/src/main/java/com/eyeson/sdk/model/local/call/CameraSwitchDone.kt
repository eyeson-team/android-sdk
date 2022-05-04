package com.eyeson.sdk.model.local.call

import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class CameraSwitchDone(val isFrontCamera: Boolean) : LocalBaseCommand