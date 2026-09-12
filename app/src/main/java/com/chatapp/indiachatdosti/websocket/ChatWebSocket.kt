package com.chatapp.indiachatdosti.websocket

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class ChatWebSocket {

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    fun connect(
        username: String,
        gender: String,
        location: String,
        onConnected: () -> Unit,
        onMessage: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        // Diagnostic only: bypass StompProtocolAndroid and test the native WebSocket handshake directly.
        // If onOpen fires, Render/Spring /ws-native is accepting a standard WebSocket handshake.
        val url = "wss://indiachatdosti.onrender.com/ws-native"

        val request = Request.Builder()
            .url(url)
            .header("Origin", "https://indiachatdosti.onrender.com")
            .build()

        println("OKHTTP WS: connecting to $url")

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                println("OKHTTP WS: onOpen - HTTP ${response.code}")
                println("OKHTTP WS: response=$response")
                onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                println("OKHTTP WS: message=$text")
                onMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                println("OKHTTP WS: binary message received")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                println("OKHTTP WS: closing code=$code reason=$reason")
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                println("OKHTTP WS: closed code=$code reason=$reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                println("OKHTTP WS: FAILURE type=${t.javaClass.name} message=${t.message}")
                println("OKHTTP WS: failure response=$response")
                t.printStackTrace()
                onError(t)
            }
        })
    }

    fun sendMessage(
        username: String,
        content: String
    ) {
        // Diagnostic only. Do not expect STOMP routing to work in this version.
        val sent = webSocket?.send(content) ?: false
        println("OKHTTP WS: raw message sent=$sent content=$content")
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }
}
