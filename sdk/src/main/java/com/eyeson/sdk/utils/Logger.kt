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

        private fun log(priority: Int, message: String, force: Boolean) {
            if (enabled || force) {
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
        }

        @JvmStatic
        fun d(message: String, force: Boolean = false) {
            log(Log.DEBUG, message, force)
        }

        @JvmStatic
        fun e(message: String, force: Boolean = false) {
            log(Log.ERROR, message, force)
        }

        @JvmStatic
        fun i(message: String, force: Boolean = false) {
            log(Log.INFO, message, force)
        }

        @JvmStatic
        fun v(message: String, force: Boolean = false) {
            log(Log.VERBOSE, message, force)
        }

        @JvmStatic
        fun w(message: String, force: Boolean = false) {
            log(Log.WARN, message, force)
        }

        @JvmStatic
        fun wtf(message: String, force: Boolean = false) {
            log(Log.ASSERT, message, force)
        }
    }
}