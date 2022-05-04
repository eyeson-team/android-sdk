package com.eyeson.sdk.model.sepp.base

import com.eyeson.sdk.model.local.base.UnknownCommand
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal class UnknownCommandDto(
    override val type: String = "Unknown command"
) : SeppBaseCommandDto {

    override fun toLocal(): UnknownCommand {
        return UnknownCommand()
    }
}