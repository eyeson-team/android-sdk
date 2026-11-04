package com.eyeson.sdk.utils

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Routes [Logger] to stdout, since android.util.Log is not available in local unit tests, and
 * keeps the logged messages so tests can assert on them. Call [install] before code that logs.
 */
internal object TestLogger {
    // Written from OkHttp and coroutine threads.
    val messages: MutableList<String> = CopyOnWriteArrayList()

    fun install() {
        messages.clear()
        Logger.printer = { _, tag, message ->
            println("$tag: $message")
            messages += message
        }
    }
}
