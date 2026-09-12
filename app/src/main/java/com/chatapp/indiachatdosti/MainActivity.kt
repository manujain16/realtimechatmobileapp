package com.chatapp.indiachatdosti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

private val PurpleStart = Color(0xFF667EEA)
private val PurpleEnd = Color(0xFF764BA2)
private val PageBackground = Color(0xFFF8F9FA)
private val TextDark = Color(0xFF333333)
private val TextMuted = Color(0xFF666666)
private val States = listOf("Andhra Pradesh","Arunachal Pradesh","Assam","Bihar","Chhattisgarh","Goa","Gujarat","Haryana","Himachal Pradesh","Jharkhand","Karnataka","Kerala","Madhya Pradesh","Maharashtra","Manipur","Meghalaya","Mizoram","Nagaland","Odisha","Punjab","Rajasthan","Sikkim","Tamil Nadu","Telangana","Tripura","Uttar Pradesh","Uttarakhand","West Bengal","Delhi","Jammu and Kashmir","Ladakh","Puducherry","Chandigarh","Andaman and Nicobar","Dadra and Nagar Haveli","Daman and Diu","Lakshadweep")

data class DisplayMessage(val sender:String,val content:String,val type:String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { ChatApp() } }
}

@Composable
fun ChatApp(vm: ChatViewModel = viewModel()) {
    var joined by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    if (joined) ChatScreen(vm, username) else LoginScreen(username,gender,location,{username=it},{gender=it},{location=it}) { vm.connect(username,gender,location); joined=true }
}

@Composable
fun LoginScreen(username:String,gender:String,location:String,onUsername:(String)->Unit,onGender:(String)->Unit,onLocation:(String)->Unit,onJoin:()->Unit) {
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(PurpleStart,PurpleEnd))).padding(20.dp)), contentAlignment=Alignment.Center) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Color.White).padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Text("💬 Real-Time Chat",fontSize=28.sp,fontWeight=FontWeight.Bold,color=TextDark)
            Spacer(Modifier.height(8.dp)); Text("Enter your name to join the conversation",color=TextMuted)
            Spacer(Modifier.height(24.dp)); OutlinedTextField(username,onUsername,placeholder={Text("Enter your username...")},singleLine=true,modifier=Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp)); Dropdown("👤 Select Your Gender",gender,listOf("Male","Female"),"Choose gender...",onGender)
            Spacer(Modifier.height(14.dp)); Dropdown("📍 Select Your Location",location,States,"Choose your state...",onLocation)
            Spacer(Modifier.height(20.dp)); Button(onClick=onJoin,enabled=username.isNotBlank()&&gender.isNotBlank()&&location.isNotBlank(),modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.buttonColors(containerColor=PurpleStart)) { Text("Join Chat",fontWeight=FontWeight.SemiBold) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dropdown(label:String,value:String,options:List<String>,placeholder:String,onSelect:(String)->Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded,{expanded=!expanded},Modifier.fillMaxWidth()) {
        OutlinedTextField(value,{},readOnly=true,label={Text(label)},placeholder={Text(placeholder)},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(expanded)},modifier=Modifier.menuAnchor().fillMaxWidth(),singleLine=true)
        ExposedDropdownMenu(expanded,{expanded=false}) { options.forEach { option -> DropdownMenuItem(text={Text(option)},onClick={onSelect(option);expanded=false}) } }
    }
}

@Composable
fun ChatScreen(vm:ChatViewModel,username:String) {
    var text by remember { mutableStateOf("") }
    var showUsers by remember { mutableStateOf(false) }
    var privateUser by remember { mutableStateOf<String?>(null) }
    val public = vm.messages.mapNotNull { parse(it) }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(PurpleStart,PurpleEnd))).padding(16.dp)) {
            Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
                Text("💬 Chat Room",color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Bold)
                Text("Welcome, $username!",color=Color.White.copy(.9f),fontSize=14.sp)
                Text("● Connected",color=Color.White.copy(.9f),fontSize=12.sp)
            }
            TextButton(onClick={showUsers=true},modifier=Modifier.align(Alignment.CenterEnd)) { Text("👥 ${vm.onlineUsers.size}",color=Color.White) }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().background(PageBackground).padding(16.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            items(public) { m -> if(m.type=="JOIN"||m.type=="LEAVE") Event(m) else if(m.content.isNotBlank()) Bubble(m,m.sender==username) }
        }
        Row(Modifier.fillMaxWidth().background(Color.White).padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
            TextButton(onClick={text += "😊"}) { Text("😊",fontSize=22.sp) }
            OutlinedTextField(text,{text=it},placeholder={Text("Type a message...")},singleLine=true,modifier=Modifier.weight(1f))
            Spacer(Modifier.width(8.dp)); Button(onClick={vm.sendMessage(text.trim());text=""},enabled=vm.connected.value&&text.isNotBlank(),colors=ButtonDefaults.buttonColors(containerColor=PurpleStart)) { Text("Send") }
        }
    }
    if(showUsers) OnlineUsersDialog(vm,username,{showUsers=false},{privateUser=it;showUsers=false})
    privateUser?.let { PrivateChatDialog(vm,username,it,{privateUser=null}) }
}

fun parse(raw:String):DisplayMessage? = try { val j=JSONObject(raw); DisplayMessage(j.optString("sender"),j.optString("content"),j.optString("type")) } catch(_:Exception){null}

@Composable fun Bubble(m:DisplayMessage,own:Boolean) { Column(Modifier.fillMaxWidth().padding(vertical=4.dp),horizontalAlignment=if(own)Alignment.End else Alignment.Start) { if(!own) Text(m.sender,color=TextMuted,fontSize=13.sp,fontWeight=FontWeight.SemiBold); Box(Modifier.clip(RoundedCornerShape(12.dp)).background(if(own)Brush.linearGradient(listOf(PurpleStart,PurpleEnd)) else Brush.linearGradient(listOf(Color.White,Color.White))).padding(14.dp)) { Text(m.content,color=if(own)Color.White else TextDark) } } }
@Composable fun Event(m:DisplayMessage) { Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFE3F2FD)).padding(9.dp)) { Text("${m.sender} ${if(m.type=="JOIN")"joined" else "left"} the chat",color=Color(0xFF1976D2),modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center,fontSize=13.sp) } }

@Composable
fun OnlineUsersDialog(vm:ChatViewModel,me:String,onClose:()->Unit,onSelect:(String)->Unit) {
    AlertDialog(onDismissRequest=onClose,title={Text("👥 Online Users")},text={Column { Text("${vm.onlineUsers.size} users online",color=TextMuted); Spacer(Modifier.height(8.dp)); LazyColumn { items(vm.onlineUsers.filter{it!=me}) { user -> Row(Modifier.fillMaxWidth().padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically) { Text("🟢 $user",modifier=Modifier.weight(1f)); TextButton(onClick={onSelect(user)}) { Text("Chat") } } } } }},confirmButton={TextButton(onClick=onClose){Text("Close")}})
}

@Composable
fun PrivateChatDialog(vm:ChatViewModel,me:String,recipient:String,onClose:()->Unit) {
    var text by remember { mutableStateOf("") }
    val msgs=vm.privateMessages.mapNotNull{parse(it)}.filter{it.sender==recipient||it.sender==me}
    AlertDialog(onDismissRequest=onClose,title={Text("💬 Private Chat with $recipient")},text={Column(Modifier.fillMaxWidth().height(360.dp)) { LazyColumn(Modifier.weight(1f)) { items(msgs) { m -> Bubble(m,m.sender==me) } }; Row(verticalAlignment=Alignment.CenterVertically){ OutlinedTextField(text,{text=it},placeholder={Text("Private message...")},singleLine=true,modifier=Modifier.weight(1f)); Spacer(Modifier.width(5.dp)); Button(onClick={vm.sendPrivateMessage(recipient,text.trim());text=""},enabled=text.isNotBlank(),colors=ButtonDefaults.buttonColors(containerColor=PurpleStart)){Text("Send")} } }},confirmButton={TextButton(onClick=onClose){Text("Close")}})
}
