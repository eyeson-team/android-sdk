package com.eyeson.sdk.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder

/**
 * Serializes a nested object as a JSON-encoded string field, e.g. ActionCable's `identifier`.
 *
 * The inner JSON uses the outer [kotlinx.serialization.json.Json], so its configuration
 * (`encodeDefaults`, `ignoreUnknownKeys`, ...) applies to the string's content too.
 */
internal class AsStringSerializer<T>(
    private val dataSerializer: KSerializer<T>
) : KSerializer<T> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("AsString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) {
        val json = (encoder as JsonEncoder).json
        encoder.encodeString(json.encodeToString(dataSerializer, value))
    }

    override fun deserialize(decoder: Decoder): T {
        val json = (decoder as JsonDecoder).json
        return json.decodeFromString(dataSerializer, decoder.decodeString())
    }
}
