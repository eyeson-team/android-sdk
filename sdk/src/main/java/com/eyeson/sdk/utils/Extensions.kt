package com.eyeson.sdk.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.launch

internal fun <T> SharedFlow<T>.collectIn(
    scope: CoroutineScope,
    block: suspend (value: T) -> Unit
) {
    scope.launch {
        buffer().collect {
            block(it)
        }
    }
}