package com.eyeson.sdk.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.launch

internal fun <T> Flow<T>.collectIn(
    scope: CoroutineScope,
    block: suspend (value: T) -> Unit
) {
    scope.launch {
        buffer().collect {
            block(it)
        }
    }
}