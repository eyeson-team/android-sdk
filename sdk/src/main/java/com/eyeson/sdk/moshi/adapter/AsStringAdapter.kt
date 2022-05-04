package com.eyeson.sdk.moshi.adapter

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.internal.Util
import org.json.JSONObject
import java.lang.reflect.Type

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
internal annotation class AsString

// Based on https://stackoverflow.com/a/56781993
internal class AsStringAdapter<T>(
    private val originAdapter: JsonAdapter<T>,
    private val stringAdapter: JsonAdapter<String>
) : JsonAdapter<T>() {
    override fun toJson(writer: JsonWriter, value: T?) {
        val jsonValue = originAdapter.toJsonValue(value)
        val jsonStr = JSONObject(jsonValue as Map<*, *>).toString()
        stringAdapter.toJson(writer, jsonStr)
    }

    override fun fromJson(reader: JsonReader): T? {
        return originAdapter.fromJson(reader.nextString())
    }

    companion object {
        var FACTORY: Factory = object : Factory {
            override fun create(
                type: Type,
                annotations: MutableSet<out Annotation>,
                moshi: Moshi
            ): JsonAdapter<*>? {
                val nextAnnotations = Types.nextAnnotations(annotations, AsString::class.java)
                return if (nextAnnotations == null || nextAnnotations.isNotEmpty())
                    null else {
                    AsStringAdapter(
                        moshi.nextAdapter<Any>(this, type, nextAnnotations),
                        moshi.nextAdapter(this, String::class.java, Util.NO_ANNOTATIONS)
                    )
                }
            }
        }
    }
}