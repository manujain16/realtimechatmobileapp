package com.chatapp.indiachatdosti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatapp.indiachatdosti.viewmodel.ChatViewModel
import org.json.JSONObject

private val GradientStart = Color(0xFF667EEA)
private val GradientEnd = Color(0xFF764BA2)
private val PageBackground = Color(0xFFF8F9FA)
private val BorderColor = Color(0xFFE0E0E0)
private val TextDark = Color(0xFF333333)
private val TextMuted = Color(0xFF666666)

private val IndianStates = listOf(
    "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh",
    "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka",
    "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya", "Mizoram",
    "Nagaland", "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu",
    "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand", "West Bengal", "Delhi",
    "Jammu and Kashmir", "Ladakh", "Puducherry", "Chandigarh", "Andaman and Nicobar",
    "Dadra and Nagar Haveli", "Daman and Diu", "Lakshadweep"
)

private data class DisplayMessage(
    val sender: String,
    val content: String,
    val type: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatApp()
        }
    }
}

@Composable
fun ChatApp(chatViewModel: ChatViewModel = viewModel()) {
    var username by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var joined by remember { mutableStateOf(false) }

    if (joined) {
        ChatScreen(chatViewModel = chatViewModel, username = username)
    } else {
        LoginScreen(
            username = username,
            gender = gender,
            location = location,
            onUsernameChange = { username = it },
            onGenderChange = { gender = it },
            onLocationChange = { location = it },
            onEnterChat = {
                if (username.isNotBlank() && gender.isNotBlank() && location.isNotBlank()) {
                    chatViewModel.connect(username, gender, location)
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(GradientStart, GradientEnd)))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(Color.White)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "💬 Real-Time Chat",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your name to join the conversation",
                color = TextMuted,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                placeholder = { Text("Enter your username...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            WebsiteDropdown(
                label = "👤 Select Your Gender",
                value = gender,
                options = listOf("Male", "Female"),
                placeholder = "Choose gender...",
                onSelected = onGenderChange
            )

            Spacer(modifier = Modifier.height(14.dp))

            WebsiteDropdown(
                label = "📍 Select Your Location",
                value = location,
                options = IndianStates,
                placeholder = "Choose your state...",
                onSelected = onLocationChange
            )

            Spacer(modifier = Modifier.height(20.dp))

            GradientButton(
                text = "Join Chat",
                enabled = username.isNotBlank() && gender.isNotBlank() && location.isNotBlank(),
                onClick = onEnterChat
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebsiteDropdown(
    label: String,
    value: String,
    options: List<String>,
    placeholder: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun GradientButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GradientStart,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFBDBDBD),
            disabledContentColor = Color.White
        )
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ChatScreen(chatViewModel: ChatViewModel, username: String) {
    var messageText by remember { mutableStateOf("") }

    val displayMessages = chatViewModel.messages.mapNotNull { raw ->
        try {
            val json = JSONObject(raw)
            DisplayMessage(
                sender = json.optString("sender"),
                content = json.optString("content"),
                type = json.optString("type")
            )
        } catch (_: Exception) {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(GradientStart, GradientEnd)))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "💬 Chat Room",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Welcome, $username!",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp
                )
            }
        }

        if (!chatViewModel.connected.value) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⏳  Connecting to chat...", color = GradientStart, fontSize = 14.sp)
            }
        }

        chatViewModel.error.value?.let { errorMessage ->
            Text(
                text = "Error: $errorMessage",
                color = Color(0xFFD32F2F),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(PageBackground),
            reverseLayout = false,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            items(displayMessages) { message ->
                if (message.type == "JOIN" || message.type == "LEAVE") {
                    EventMessage(message)
                } else if (message.content.isNotBlank()) {
                    ChatBubble(message = message, ownMessage = message.sender == username)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { messageText += "😊" },
                    modifier = Modifier.size(48.dp)
                ) {
                    Text("😊", fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Type a message...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            chatViewModel.sendMessage(messageText.trim())
                            messageText = ""
                        }
                    },
                    enabled = chatViewModel.connected.value && messageText.isNotBlank(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GradientStart)
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: DisplayMessage, ownMessage: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalAlignment = if (ownMessage) Alignment.End else Alignment.Start
    ) {
        if (!ownMessage) {
            Text(
                text = message.sender,
                color = TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (ownMessage) {
                        Brush.linearGradient(listOf(GradientStart, GradientEnd))
                    } else {
                        Brush.linearGradient(listOf(Color.White, Color.White))
                    }
                )
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Text(
                text = message.content,
                color = if (ownMessage) Color.White else TextDark,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun EventMessage(message: DisplayMessage) {
    val action = if (message.type == "JOIN") "joined the chat" else "left the chat"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE3F2FD))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${message.sender} $action",
            color = Color(0xFF1976D2),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}
