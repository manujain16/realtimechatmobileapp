package com.chatapp.indiachatdosti.websocket

import org.json.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

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
        onMessage: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val url = "wss://indiachatdosti.onrender.com/ws-native"

        val request = Request.Builder()
            .url(url)
            .header("Origin", "https://indiachatdosti.onrender.com")
            .header("Sec-WebSocket-Protocol", "v12.stomp")
            .build()

        println("OKHTTP STOMP: connecting to $url")

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                println("OKHTTP STOMP: WebSocket OPEN HTTP ${response.code}")
                sendStompConnect(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                println("OKHTTP STOMP: received=$text")
                processIncoming(text, webSocket, username, gender, location, onConnected, onMessage)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                processIncoming(bytes.utf8(), webSocket, username, gender, location, onConnected, onMessage)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                println("OKHTTP STOMP: closing code=$code reason=$reason")
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                stompConnected = false
                println("OKHTTP STOMP: closed code=$code reason=$reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                stompConnected = false
                println("OKHTTP STOMP: FAILURE ${t.javaClass.name}: ${t.message}")
                println("OKHTTP STOMP: failure response=$response")
                t.printStackTrace()
                onError(t)
            }
        })
    }

    private fun sendStompConnect(webSocket: WebSocket) {
        val frame = buildFrame(
            command = "CONNECT",
            headers = listOf(
                "accept-version" to "1.2",
                "host" to "indiachatdosti.onrender.com",
                "heart-beat" to "10000,10000"
            )
        )
        println("OKHTTP STOMP: sending CONNECT")
        webSocket.send(frame)
    }

    private fun processIncoming(
        text: String,
        webSocket: WebSocket,
        username: String,
        gender: String,
        location: String,
        onConnected: () -> Unit,
        onMessage: (String) -> Unit
    ) {
        incomingBuffer += text

        while (incomingBuffer.contains('\u0000')) {
            val end = incomingBuffer.indexOf('\u0000')
            val rawFrame = incomingBuffer.substring(0, end)
            incomingBuffer = incomingBuffer.substring(end + 1)

            if (rawFrame.isBlank()) continue
            handleStompFrame(
                rawFrame,
                webSocket,
                username,
                gender,
                location,
                onConnected,
                onMessage
            )
        }
    }

    private fun handleStompFrame(
        frame: String,
        webSocket: WebSocket,
        username: String,
        gender: String,
        location: String,
        onConnected: () -> Unit,
        onMessage: (String) -> Unit
    ) {
        val lines = frame.split("\n")
        val command = lines.firstOrNull()?.trim() ?: return
        println("OKHTTP STOMP: frame=$command")

        when (command) {
            "CONNECTED" -> {
                stompConnected = true
                println("OKHTTP STOMP: STOMP CONNECTED")

                webSocket.send(
                    buildFrame(
                        "SUBSCRIBE",
                        listOf(
                            "id" to "public-chat",
                            "destination" to "/topic/public",
                            "ack" to "auto"
                        )
                    )
                )

                val joinJson = JSONObject()
                    .put("sender", username)
                    .put("gender", gender)
                    .put("location", location)
                    .put("type", "JOIN")
                    .toString()

                webSocket.send(
                    buildFrame(
                        "SEND",
                        listOf(
                            "destination" to "/app/chat.addUser",
                            "content-type" to "application/json"
                        ),
                        joinJson
                    )
                )

                onConnected()
            }

            "MESSAGE" -> {
                val bodyStart = frame.indexOf("\n\n")
                if (bodyStart >= 0) {
                    val body = frame.substring(bodyStart + 2)
                    if (body.isNotBlank()) {
                        onMessage(body)
                    }
                }
            }

            "ERROR" -> {
                val bodyStart = frame.indexOf("\n\n")
                val details = if (bodyStart >= 0) frame.substring(bodyStart + 2) else frame
                println("OKHTTP STOMP: SERVER ERROR $details")
            }

            "RECEIPT" -> println("OKHTTP STOMP: RECEIPT")
            "\n" -> Unit
        }
    }

    private fun buildFrame(
        command: String,
        headers: List<Pair<String, String>>,
        body: String = ""
    ): String {
        val headerText = headers.joinToString("\n") { "${it.first}:${it.second}" }
        return if (body.isEmpty()) {
            "$command\n$headerText\n\n\u0000"
        } else {
            "$command\n$headerText\ncontent-length:${body.toByteArray(Charsets.UTF_8).size}\n\n$body\u0000"
        }
    }

    fun sendMessage(
        username: String,
        content: String
    ) {
        if (!stompConnected) {
            println("OKHTTP STOMP: cannot send; STOMP is not connected")
            return
        }

        val json = JSONObject()
            .put("sender", username)
            .put("content", content)
            .put("type", "CHAT")
            .toString()

        val frame = buildFrame(
            "SEND",
            listOf(
                "destination" to "/app/chat.sendMessage",
                "content-type" to "application/json"
            ),
            json
        )

        val sent = webSocket?.send(frame) ?: false
        println("OKHTTP STOMP: message sent=$sent")
    }

    fun disconnect() {
        stompConnected = false
        webSocket?.send("DISCONNECT\n\n\u0000")
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        incomingBuffer = ""
    }
}
