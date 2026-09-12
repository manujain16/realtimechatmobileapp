package com.chatapp.indiachatdosti.viewmodel

import androidx.lifecycle.ViewModel
import com.chatapp.indiachatdosti.websocket.ChatWebSocket

class ChatViewModel : ViewModel() {

    private val chatWebSocket = ChatWebSocket()

    fun connect(
        username: String,
        gender: String,
        location: String
    ) {
        chatWebSocket.connect(
            username = username,
            gender = gender,
            location = location,
            onConnected = {
                // Handle connection
            },
            onMessage = {
                // Handle message
            },
            onError = {
                // Handle error
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        chatWebSocket.disconnect()
    }
}
