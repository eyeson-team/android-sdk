package com.eyeson.sdk.model.local.meeting

import com.eyeson.sdk.model.local.api.MeetingOptions
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.base.LocalBaseCommand

data class OptionsUpdate(val meetingOptions: MeetingOptions) : LocalBaseCommand
