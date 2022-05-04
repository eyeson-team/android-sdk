package com.eyeson.sdk.exceptions.internal

internal class FaultyInfoException(val code: Int) : Exception("$code")