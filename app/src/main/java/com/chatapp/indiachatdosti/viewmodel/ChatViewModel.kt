package com.chatapp.indiachatdosti.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.chatapp.indiachatdosti.websocket.ChatWebSocket
import org.json.JSONObject

class ChatViewModel : ViewModel() {
    private val chatWebSocket = ChatWebSocket()
    val connected = mutableStateOf(false)
    val messages = mutableStateListOf<String>()
    val privateMessages = mutableStateListOf<String>()
    val onlineUsers = mutableStateListOf<String>()
    val userGenders = mutableStateOf<Map<String, String>>(emptyMap())
    val error = mutableStateOf<String?>(null)
    val incomingPrivateUser = mutableStateOf<String?>(null)
    private var currentUsername = ""

    fun connect(username: String, gender: String, location: String) {
        currentUsername = username
        error.value = null
        incomingPrivateUser.value = null
        messages.clear(); privateMessages.clear(); onlineUsers.clear()
        chatWebSocket.connect(
            username = username, gender = gender, location = location,
            onConnected = { connected.value = true },
            onPublicMessage = { message -> messages.add(message); updateUsers(message) },
            onPrivateMessage = { message ->
                privateMessages.add(message)
                try {
                    val sender = JSONObject(message).optString("sender")
                    if (sender.isNotBlank() && sender != currentUsername) {
                        incomingPrivateUser.value = sender
                    }
                } catch (_: Exception) { }
            },
            onError = { throwable -> connected.value = false; error.value = throwable.message ?: "WebSocket connection error" }
        )
    }

    fun clearIncomingPrivateUser() {
        incomingPrivateUser.value = null
    }

    private fun updateUsers(raw: String) {
        try {
            val json = JSONObject(raw)
            json.optJSONArray("onlineUsers")?.let { users ->
                onlineUsers.clear()
                for (i in 0 until users.length()) onlineUsers.add(users.getString(i))
            }
            json.optJSONObject("userGenders")?.let { genders ->
                val map = mutableMapOf<String, String>()
                genders.keys().forEach { map[it] = genders.optString(it) }
                userGenders.value = map
            }
        } catch (_: Exception) { }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || !connected.value) return
        chatWebSocket.sendMessage(currentUsername, content)
    }

    fun sendPrivateMessage(recipient: String, content: String) {
        if (recipient.isBlank() || content.isBlank() || !connected.value) return
        chatWebSocket.sendPrivateMessage(currentUsername, recipient, content)
    }

    fun disconnect() { connected.value = false; chatWebSocket.disconnect() }
    override fun onCleared() { super.onCleared(); chatWebSocket.disconnect() }
}
