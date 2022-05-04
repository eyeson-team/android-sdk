package com.eyeson.sdk.utils

import android.os.Build
import android.util.Log
import com.eyeson.sdk.BuildConfig.DEBUG
import java.util.regex.Pattern

internal class Logger {
    companion object {
        @JvmStatic
        var enabled = DEBUG
        private const val MAX_TAG_LENGTH = 23

        private fun log(priority: Int, message: String) {
            if (!enabled) {
                return
            }

            val tag = Throwable().stackTrace
                .first { it.className != this::class.java.name }
                .let {
                    var tag = it.className.substringAfterLast('.')
                    Pattern.compile("(\\$\\d+)+$").apply {
                        val matcher = matcher(tag)
                        if (matcher.find()) {
                            tag = matcher.replaceAll("")
                        }
                    }

                    if (Build.VERSION.SDK_INT < 26) {
                        tag.take(MAX_TAG_LENGTH)
                    } else {
                        tag
                    }
                }

            Log.println(priority, tag, message)
        }

        @JvmStatic
        fun d(message: String) {
            log(Log.DEBUG, message)
        }

        @JvmStatic
        fun e(message: String) {
            log(Log.ERROR, message)
        }

        @JvmStatic
        fun i(message: String) {
            log(Log.INFO, message)
        }

        @JvmStatic
        fun v(message: String) {
            log(Log.VERBOSE, message)
        }

        @JvmStatic
        fun w(message: String) {
            log(Log.WARN, message)
        }

        @JvmStatic
        fun wtf(message: String) {
            log(Log.ASSERT, message)
        }

    }
}