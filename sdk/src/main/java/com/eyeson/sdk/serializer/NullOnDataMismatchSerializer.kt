package com.eyeson.sdk.serializer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder

/**
 * Decodes to `null` instead of failing the enclosing object when [delegate] can't decode the
 * value, e.g. the numeric `message` of ActionCable pings.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class NullOnDataMismatchSerializer<T : Any>(
    private val delegate: KSerializer<T>
) : KSerializer<T?> {
    override val descriptor: SerialDescriptor = delegate.descriptor.nullable

    override fun serialize(encoder: Encoder, value: T?) {
        encoder.encodeNullableSerializableValue(delegate, value)
    }

    override fun deserialize(decoder: Decoder): T? {
        // JSON null, or a missing key that `explicitNulls = false` decodes as null
        if (!decoder.decodeNotNullMark()) return decoder.decodeNull()

        val jsonDecoder = decoder as JsonDecoder
        // Decoding the element first leaves the outer decoder in a known state on failure.
        val element = jsonDecoder.decodeJsonElement()
        return try {
            jsonDecoder.json.decodeFromJsonElement(delegate, element)
        } catch (e: IllegalArgumentException) {
            // SerializationException is an IllegalArgumentException
            null
        }
    }
}
