package com.chatapp.indiachatdosti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatapp.indiachatdosti.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ChatApp()
        }
    }
}

@Composable
fun ChatApp(
    chatViewModel: ChatViewModel = viewModel()
) {
    var username by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var joined by remember { mutableStateOf(false) }

    if (joined) {
        ChatScreen(
            chatViewModel = chatViewModel,
            username = username
        )
    } else {
        LoginScreen(
            username = username,
            gender = gender,
            location = location,
            onUsernameChange = { username = it },
            onGenderChange = { gender = it },
            onLocationChange = { location = it },
            onEnterChat = {
                if (
                    username.isNotBlank() &&
                    gender.isNotBlank() &&
                    location.isNotBlank()
                ) {
                    chatViewModel.connect(
                        username = username,
                        gender = gender,
                        location = location
                    )
                    joined = true
                }
            }
        )
    }
}

@Composable
fun LoginScreen(
    username: String,
    gender: String,
    location: String,
    onUsernameChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onEnterChat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "India Chat Dosti")

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = gender,
            onValueChange = onGenderChange,
            label = { Text("Gender") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = location,
            onValueChange = onLocationChange,
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onEnterChat,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ENTER CHAT")
        }
    }
}

@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    username: String
) {
    var messageText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "India Chat Dosti")

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (chatViewModel.connected.value)
                "Connected"
            else
                "Connecting..."
        )

        chatViewModel.error.value?.let { errorMessage ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Error: $errorMessage")
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(chatViewModel.messages) { message ->
                Text(
                    text = message,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                label = { Text("Message") },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.padding(4.dp))

            Button(
                onClick = {
                    if (messageText.isNotBlank()) {
                        chatViewModel.sendMessage(messageText)
                        messageText = ""
                    }
                },
                enabled = chatViewModel.connected.value
            ) {
                Text("SEND")
            }
        }
    }
}
