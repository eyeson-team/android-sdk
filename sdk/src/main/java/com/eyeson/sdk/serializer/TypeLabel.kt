package com.eyeson.sdk.serializer

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The `type` label incoming messages are dispatched on, or `null` if this isn't an object or the
 * label is missing or not a string.
 */
internal fun JsonElement.typeLabel(): String? {
    val label = (this as? JsonObject)?.get("type") as? JsonPrimitive
    return label?.takeIf { it.isString }?.content
}
