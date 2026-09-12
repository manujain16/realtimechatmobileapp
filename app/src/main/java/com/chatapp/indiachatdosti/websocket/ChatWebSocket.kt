package com.chatapp.indiachatdosti.websocket

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject

class ChatWebSocket {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private var stompConnected = false
    private var incomingBuffer = ""

    fun connect(
        username: String,
        gender: String,
        location: String,
        onConnected: () -> Unit,
        onPublicMessage: (String) -> Unit,
        onPrivateMessage: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val url = "wss://indiachatdosti.onrender.com/ws-native"
        val request = Request.Builder()
            .url(url)
            .header("Origin", "https://indiachatdosti.onrender.com")
            .header("Sec-WebSocket-Protocol", "v12.stomp")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) = sendStompConnect(webSocket, username)
            override fun onMessage(webSocket: WebSocket, text: String) = processIncoming(text, webSocket, username, gender, location, onConnected, onPublicMessage, onPrivateMessage)
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) = processIncoming(bytes.utf8(), webSocket, username, gender, location, onConnected, onPublicMessage, onPrivateMessage)
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { stompConnected = false }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { stompConnected = false; onError(t) }
        })
    }

    private fun sendStompConnect(webSocket: WebSocket, username: String) {
        webSocket.send(buildFrame("CONNECT", listOf(
            "accept-version" to "1.2",
            "host" to "indiachatdosti.onrender.com",
            "login" to username,
            "heart-beat" to "10000,10000"
        )))
    }

    private fun processIncoming(
        text: String,
        webSocket: WebSocket,
        username: String,
        gender: String,
        location: String,
        onConnected: () -> Unit,
        onPublicMessage: (String) -> Unit,
        onPrivateMessage: (String) -> Unit
    ) {
        incomingBuffer += text
        while (incomingBuffer.contains('\u0000')) {
            val end = incomingBuffer.indexOf('\u0000')
            val frame = incomingBuffer.substring(0, end)
            incomingBuffer = incomingBuffer.substring(end + 1)
            if (frame.isNotBlank()) handleFrame(frame, webSocket, username, gender, location, onConnected, onPublicMessage, onPrivateMessage)
        }
    }

    private fun handleFrame(
        frame: String,
        webSocket: WebSocket,
        username: String,
        gender: String,
        location: String,
        onConnected: () -> Unit,
        onPublicMessage: (String) -> Unit,
        onPrivateMessage: (String) -> Unit
    ) {
        when (frame.substringBefore('\n').trim()) {
            "CONNECTED" -> {
                stompConnected = true
                webSocket.send(buildFrame("SUBSCRIBE", listOf("id" to "public-chat", "destination" to "/topic/public", "ack" to "auto")))
                webSocket.send(buildFrame("SUBSCRIBE", listOf("id" to "private-chat", "destination" to "/user/queue/private", "ack" to "auto")))
                val join = JSONObject().put("sender", username).put("gender", gender).put("location", location).put("type", "JOIN").toString()
                webSocket.send(buildFrame("SEND", listOf("destination" to "/app/chat.addUser", "content-type" to "application/json"), join))
                onConnected()
            }
            "MESSAGE" -> {
                val bodyStart = frame.indexOf("\n\n")
                if (bodyStart >= 0) {
                    val body = frame.substring(bodyStart + 2).trimEnd('\r')
                    if (body.isNotBlank()) {
                        val destination = frame.lineSequence().firstOrNull { it.startsWith("destination:") }?.substringAfter(":") ?: ""
                        if (destination == "/user/queue/private" || destination.startsWith("/user/")) onPrivateMessage(body)
                        else onPublicMessage(body)
                    }
                }
            }
        }
    }

    private fun buildFrame(command: String, headers: List<Pair<String, String>>, body: String = ""): String {
        val headerText = headers.joinToString("\n") { "${it.first}:${it.second}" }
        return if (body.isEmpty()) "$command\n$headerText\n\n\u0000"
        else "$command\n$headerText\ncontent-length:${body.toByteArray(Charsets.UTF_8).size}\n\n$body\u0000"
    }

    fun sendMessage(username: String, content: String, imageData: String? = null) {
        if (!stompConnected) return
        val json = JSONObject()
            .put("sender", username)
            .put("content", content)
            .put("type", "CHAT")
        if (!imageData.isNullOrBlank()) json.put("imageData", imageData)
        webSocket?.send(buildFrame("SEND", listOf("destination" to "/app/chat.sendMessage", "content-type" to "application/json"), json.toString()))
    }

    fun sendPrivateMessage(username: String, recipient: String, content: String, imageData: String? = null) {
        if (!stompConnected) return
        val json = JSONObject()
            .put("sender", username)
            .put("recipient", recipient)
            .put("content", content)
            .put("type", "PRIVATE_MESSAGE")
        if (!imageData.isNullOrBlank()) json.put("imageData", imageData)
        webSocket?.send(buildFrame("SEND", listOf("destination" to "/app/chat.sendPrivateMessage", "content-type" to "application/json"), json.toString()))
    }

    fun disconnect() {
        stompConnected = false
        webSocket?.send("DISCONNECT\n\n\u0000")
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        incomingBuffer = ""
    }
}
