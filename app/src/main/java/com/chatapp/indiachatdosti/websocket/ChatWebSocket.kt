package com.chatapp.indiachatdosti.websocket

import io.reactivex.disposables.Disposable
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient

class ChatWebSocket {

    private lateinit var stompClient: StompClient
    private var publicSubscription: Disposable? = null

    fun connect(
        username: String,
        gender: String,
        location: String,
        onConnected: () -> Unit,
        onMessage: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val url = "wss://indiachatdosti.onrender.com/ws"

        stompClient = Stomp.over(
            Stomp.ConnectionProvider.OKHTTP,
            url
        )

        stompClient.connect()

        stompClient.lifecycle()
            .subscribe { lifecycleEvent ->
                when (lifecycleEvent.type) {
                    ua.naiksoftware.stomp.dto.LifecycleEvent.Type.OPENED -> {
                        println("STOMP connected")
                        onConnected()
                        subscribeToPublicChat(onMessage)
                        addUser(
                            username,
                            gender,
                            location
                        )
                    }

                    ua.naiksoftware.stomp.dto.LifecycleEvent.Type.ERROR -> {
                        lifecycleEvent.exception?.let {
                            onError(it)
                        }
                    }

                    ua.naiksoftware.stomp.dto.LifecycleEvent.Type.CLOSED -> {
                        println("STOMP connection closed")
                    }

                    else -> {}
                }
            }
    }

    private fun subscribeToPublicChat(onMessage: (String) -> Unit) {
        publicSubscription = stompClient.topic("/topic/public")
            .subscribe(
                { message ->
                    println("Received: ${message.payload}")

                    message.payload?.let {
                        onMessage(it)
                    }
                },
                { error ->
                    println("Subscription error: $error")
                }
            )
    }

    private fun addUser(
        username: String,
        gender: String,
        location: String
    ) {
        val json = """
            {
                "sender": "$username",
                "gender": "$gender",
                "location": "$location",
                "type": "JOIN"
            }
        """.trimIndent()

        stompClient.send(
            "/app/chat.addUser",
            json
        ).subscribe(
            {
                println("User joined")
            },
            { error ->
                println("Join error: $error")
            }
        )
    }

    fun sendMessage(
        username: String,
        content: String
    ) {
        val json = """
            {
                "sender": "$username",
                "content": "$content",
                "type": "CHAT"
            }
        """.trimIndent()

        stompClient.send(
            "/app/chat.sendMessage",
            json
        ).subscribe(
            {
                println("Message sent")
            },
            { error ->
                println("Send error: $error")
            }
        )
    }

    fun disconnect() {
        publicSubscription?.dispose()
        if (::stompClient.isInitialized) {
            stompClient.disconnect()
        }
    }
}
