package com.chatapp.indiachatdosti.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.chatapp.indiachatdosti.websocket.ChatWebSocket

class ChatViewModel : ViewModel() {

    private val chatWebSocket = ChatWebSocket()

    val connected = mutableStateOf(false)
    val messages = mutableStateListOf<String>()
    val error = mutableStateOf<String?>(null)

    private var currentUsername = ""

    fun connect(
        username: String,
        gender: String,
        location: String
    ) {
        currentUsername = username
        error.value = null

        chatWebSocket.connect(
            username = username,
            gender = gender,
            location = location,
            onConnected = {
                connected.value = true
            },
            onMessage = { message ->
                messages.add(message)
            },
            onError = { throwable ->
                connected.value = false
                error.value = throwable.message ?: "WebSocket connection error"
            }
        )
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || !connected.value) return

        chatWebSocket.sendMessage(
            username = currentUsername,
            content = content
        )
    }

    fun disconnect() {
        connected.value = false
        chatWebSocket.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        chatWebSocket.disconnect()
    }
}
