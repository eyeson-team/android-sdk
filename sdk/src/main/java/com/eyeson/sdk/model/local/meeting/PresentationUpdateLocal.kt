package com.eyeson.sdk.model.local.meeting

import com.eyeson.sdk.model.local.api.PresentationUpdate
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class PresentationUpdateLocal(val user: UserInfo?) : LocalBaseCommand

internal fun PresentationUpdateLocal.toLocal(): PresentationUpdate {
    return PresentationUpdate(user = user)
}
