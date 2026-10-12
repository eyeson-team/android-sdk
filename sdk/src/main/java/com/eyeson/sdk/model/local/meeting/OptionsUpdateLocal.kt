package com.eyeson.sdk.model.local.meeting

import com.eyeson.sdk.model.local.api.MeetingOptions
import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class OptionsUpdateLocal(val meetingOptions: MeetingOptions) : LocalBaseCommand
