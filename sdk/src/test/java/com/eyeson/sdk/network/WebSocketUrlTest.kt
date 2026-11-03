package com.eyeson.sdk.network

import kotlin.test.Test
import kotlin.test.assertEquals

class WebSocketUrlTest {

    @Test
    fun httpsMeetingLinkBecomesWssOnDefaultPort() {
        val url = webSocketUrl("https://api.eyeson.team/rt?access_key=test-access-key")

        assertEquals("wss://api.eyeson.team/rt?access_key=test-access-key", url.toString())
        assertEquals(443, url.port)
    }

    @Test
    fun httpLinkBecomesWsAndKeepsExplicitPort() {
        assertEquals("ws://localhost:8080/rt", webSocketUrl("http://localhost:8080/rt").toString())
    }

    @Test
    fun webSocketEndpointIsKept() {
        val url = webSocketUrl("wss://sig.eyeson.com/call")

        assertEquals("wss://sig.eyeson.com/call", url.toString())
        assertEquals(443, url.port)
    }
}
