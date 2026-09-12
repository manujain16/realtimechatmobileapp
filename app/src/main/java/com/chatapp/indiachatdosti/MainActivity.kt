package com.chatapp.indiachatdosti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
            LoginScreen()
        }
    }
}

@Composable
fun LoginScreen(
    chatViewModel: ChatViewModel = viewModel()
) {

    var username by remember {
        mutableStateOf("")
    }

    var gender by remember {
        mutableStateOf("")
    }

    var location by remember {
        mutableStateOf("")
    }

    var connected by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "India Chat Dosti"
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
            },
            label = {
                Text("Username")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = gender,
            onValueChange = {
                gender = it
            },
            label = {
                Text("Gender")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = location,
            onValueChange = {
                location = it
            },
            label = {
                Text("Location")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Button(
            onClick = {

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

                    connected = true
                }

            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = if (connected)
                    "Connecting..."
                else
                    "ENTER CHAT"
            )
        }
    }
}