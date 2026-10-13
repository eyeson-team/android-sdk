package com.eyeson.sdk.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Date

/**
 * RFC 3339 timestamps such as `2024-05-01T12:34:56.789Z` or `2024-05-01T14:34:56+02:00`.
 */
internal object DateSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.eyeson.sdk.Date", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeString(DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(value.time)))
    }

    override fun deserialize(decoder: Decoder): Date {
        val value = decoder.decodeString()
        return try {
            Date(OffsetDateTime.parse(value).toInstant().toEpochMilli())
        } catch (e: DateTimeParseException) {
            throw SerializationException("Invalid RFC 3339 date '$value'", e)
        }
    }
}
