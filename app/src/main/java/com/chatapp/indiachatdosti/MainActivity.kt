package com.chatapp.indiachatdosti

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatapp.indiachatdosti.viewmodel.ChatViewModel
import org.json.JSONObject
import java.io.ByteArrayOutputStream

private val PurpleStart = Color(0xFF667EEA)
private val PurpleEnd = Color(0xFF764BA2)
private val PageBackground = Color(0xFFF8F9FA)
private val TextDark = Color(0xFF333333)
private val TextMuted = Color(0xFF666666)
private val States = listOf("Andhra Pradesh","Arunachal Pradesh","Assam","Bihar","Chhattisgarh","Goa","Gujarat","Haryana","Himachal Pradesh","Jharkhand","Karnataka","Kerala","Madhya Pradesh","Maharashtra","Manipur","Meghalaya","Mizoram","Nagaland","Odisha","Punjab","Rajasthan","Sikkim","Tamil Nadu","Telangana","Tripura","Uttar Pradesh","Uttarakhand","West Bengal","Delhi","Jammu and Kashmir","Ladakh","Puducherry","Chandigarh","Andaman and Nicobar","Dadra and Nagar Haveli","Daman and Diu","Lakshadweep")

data class DisplayMessage(val sender:String,val content:String,val type:String,val imageData:String? = null)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?){ super.onCreate(savedInstanceState); setContent{ChatApp()} }
}

@Composable fun ChatApp(vm:ChatViewModel=viewModel()){
    var joined by remember{mutableStateOf(false)}; var username by remember{mutableStateOf("")}; var gender by remember{mutableStateOf("")}; var location by remember{mutableStateOf("")}
    if(joined) ChatScreen(vm,username) else LoginScreen(username,gender,location,{username=it},{gender=it},{location=it}){vm.connect(username,gender,location);joined=true}
}

@Composable fun LoginScreen(username:String,gender:String,location:String,onUsername:(String)->Unit,onGender:(String)->Unit,onLocation:(String)->Unit,onJoin:()->Unit){
    Box(modifier=Modifier.fillMaxSize().background(Brush.linearGradient(listOf(PurpleStart,PurpleEnd))).padding(20.dp),contentAlignment=Alignment.Center){
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Color.White).padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally){
            Text("💬 Real-Time Chat",fontSize=28.sp,fontWeight=FontWeight.Bold,color=TextDark); Spacer(Modifier.height(8.dp)); Text("Enter your name to join the conversation",color=TextMuted)
            Spacer(Modifier.height(24.dp)); OutlinedTextField(username,onUsername,placeholder={Text("Enter your username...")},singleLine=true,modifier=Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp)); Dropdown("👤 Select Your Gender",gender,listOf("Male","Female"),"Choose gender...",onGender)
            Spacer(Modifier.height(14.dp)); Dropdown("📍 Select Your Location",location,States,"Choose your state...",onLocation)
            Spacer(Modifier.height(20.dp)); Button(onClick=onJoin,enabled=username.isNotBlank()&&gender.isNotBlank()&&location.isNotBlank(),modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.buttonColors(containerColor=PurpleStart)){Text("Join Chat",fontWeight=FontWeight.SemiBold)}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun Dropdown(label:String,value:String,options:List<String>,placeholder:String,onSelect:(String)->Unit){
    var expanded by remember{mutableStateOf(false)}
    ExposedDropdownMenuBox(expanded=expanded,onExpandedChange={expanded=!expanded},modifier=Modifier.fillMaxWidth()){
        OutlinedTextField(value,{},readOnly=true,label={Text(label)},placeholder={Text(placeholder)},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(expanded)},modifier=Modifier.menuAnchor().fillMaxWidth(),singleLine=true)
        ExposedDropdownMenu(expanded,{expanded=false}){options.forEach{option->DropdownMenuItem(text={Text(option)},onClick={onSelect(option);expanded=false})}}
    }
}

@Composable fun ChatScreen(vm:ChatViewModel,username:String){
    var text by remember{mutableStateOf("")}; var showUsers by remember{mutableStateOf(false)}; var privateUser by remember{mutableStateOf<String?>(null)}
    val public=vm.messages.mapNotNull{parse(it)}
    LaunchedEffect(vm.incomingPrivateUser.value){vm.incomingPrivateUser.value?.let{privateUser=it;showUsers=false;vm.clearIncomingPrivateUser()}}

    Column(Modifier.fillMaxSize().background(Color.White)){
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(PurpleStart,PurpleEnd))).padding(16.dp)){
            Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){Text("💬 Chat Room",color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Bold);Text("Welcome, $username!",color=Color.White.copy(.9f),fontSize=14.sp);Text(if(vm.connected.value)"● Connected" else "⏳ Connecting...",color=Color.White.copy(.9f),fontSize=12.sp)}
            TextButton(onClick={showUsers=true},modifier=Modifier.align(Alignment.CenterEnd)){Text("👥 ${vm.onlineUsers.size}",color=Color.White)}
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().background(PageBackground).padding(16.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){items(public){m->if(m.type=="JOIN"||m.type=="LEAVE")Event(m)else if(m.content.isNotBlank()||m.imageData!=null)Bubble(m,m.sender==username)}}
        Row(Modifier.fillMaxWidth().background(Color.White).padding(12.dp),verticalAlignment=Alignment.CenterVertically){TextButton(onClick={text+="😊"}){Text("😊",fontSize=22.sp)}; ImagePickerButton{vm.sendImage(it)}; OutlinedTextField(text,{text=it},placeholder={Text("Type a public message...")},singleLine=true,modifier=Modifier.weight(1f));Spacer(Modifier.width(8.dp));Button(onClick={vm.sendMessage(text.trim());text=""},enabled=vm.connected.value&&text.isNotBlank(),colors=ButtonDefaults.buttonColors(containerColor=PurpleStart)){Text("Send")}}
    }
    if(showUsers)OnlineUsersDialog(vm,username,{showUsers=false},{privateUser=it;showUsers=false})
    privateUser?.let{PrivateChatDialog(vm,username,it){privateUser=null}}
}

fun parse(raw:String):DisplayMessage?=try{val j=JSONObject(raw);DisplayMessage(j.optString("sender"),j.optString("content"),j.optString("type"),j.optString("imageData").ifBlank{null})}catch(_:Exception){null}

@Composable fun Bubble(m:DisplayMessage,own:Boolean){
    Column(Modifier.fillMaxWidth().padding(vertical=4.dp),horizontalAlignment=if(own)Alignment.End else Alignment.Start){
        if(!own)Text(m.sender,color=TextMuted,fontSize=13.sp,fontWeight=FontWeight.SemiBold)
        Box(Modifier.clip(RoundedCornerShape(12.dp)).background(if(own)Brush.linearGradient(listOf(PurpleStart,PurpleEnd)) else Brush.linearGradient(listOf(Color.White,Color.White))).padding(10.dp)){
            Column(horizontalAlignment=if(own)Alignment.End else Alignment.Start){
                m.imageData?.let{SharedImage(it)}
                if(m.content.isNotBlank())Text(m.content,color=if(own)Color.White else TextDark,modifier=Modifier.padding(if(m.imageData!=null) PaddingValues(top=6.dp) else PaddingValues(4.dp)))}
        }
    }
}

@Composable fun SharedImage(data:String){
    val bitmap=remember(data){decodeImage(data)}
    bitmap?.let{Image(bitmap=it.asImageBitmap(),contentDescription="Shared image",modifier=Modifier.sizeIn(maxWidth=260.dp,maxHeight=300.dp).clip(RoundedCornerShape(10.dp)),contentScale=ContentScale.Fit)}
}

fun decodeImage(data:String):Bitmap?=try{val encoded=data.substringAfter(",",data);BitmapFactory.decodeByteArray(Base64.decode(encoded,Base64.DEFAULT),0,Base64.decode(encoded,Base64.DEFAULT).size)}catch(_:Exception){null}

@Composable fun ImagePickerButton(onImage:(String)->Unit){
    val context=androidx.compose.ui.platform.LocalContext.current
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->uri?.let{prepareImageData(context,it)?.let(onImage)}}
    IconButton(onClick={launcher.launch("image/*")}){Text("📷",fontSize=22.sp)}
}

fun prepareImageData(context:Context,uri:Uri):String?=try{
    val source=context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it)} ?: return null
    val maxDimension=1280
    val scale=minOf(1f,maxDimension.toFloat()/maxOf(source.width,source.height))
    val width=(source.width*scale).toInt().coerceAtLeast(1); val height=(source.height*scale).toInt().coerceAtLeast(1)
    val bitmap=Bitmap.createScaledBitmap(source,width,height,true)
    var quality=78
    var bytes:ByteArray
    do{val out=ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,quality,out);bytes=out.toByteArray();quality-=8}while(bytes.size>500*1024&&quality>=38)
    if(bytes.size>500*1024)return null
    "data:image/jpeg;base64,"+Base64.encodeToString(bytes,Base64.NO_WRAP)
}catch(_:Exception){null}

@Composable fun Event(m:DisplayMessage){Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFE3F2FD)).padding(9.dp)){Text("${m.sender} ${if(m.type=="JOIN")"joined" else "left"} the chat",color=Color(0xFF1976D2),modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center,fontSize=13.sp)}}

@Composable fun OnlineUsersDialog(vm:ChatViewModel,me:String,onClose:()->Unit,onSelect:(String)->Unit){
    var filter by remember{mutableStateOf("All")}; val filteredUsers=vm.onlineUsers.filter{user->user!=me&&(filter=="All"||vm.userGenders.value[user].equals(filter,ignoreCase=true))}; val groupedUsers=filteredUsers.groupBy{vm.userLocations.value[it].orEmpty().ifBlank{"Unknown"}}.toSortedMap(); val expandedLocations=remember{mutableStateMapOf<String,Boolean>()}
    AlertDialog(onDismissRequest=onClose,title={Text("👥 Online Users")},text={Column(Modifier.fillMaxWidth()){Text("${filteredUsers.size} users shown • ${vm.onlineUsers.size} online",color=TextMuted);Spacer(Modifier.height(10.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("All","Male","Female").forEach{option->FilterChip(selected=filter==option,onClick={filter=option},label={Text(option)})}};Spacer(Modifier.height(8.dp));LazyColumn(Modifier.heightIn(max=360.dp)){groupedUsers.forEach{(location,users)->val expanded=expandedLocations[location]?:true;item(key="location_$location"){Surface(modifier=Modifier.fillMaxWidth().padding(top=6.dp),shape=RoundedCornerShape(8.dp),color=PageBackground,onClick={expandedLocations[location]=!expanded}){Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){Text(if(expanded)"▼" else "▶",fontSize=13.sp);Spacer(Modifier.width(7.dp));Text("📍 $location",fontWeight=FontWeight.Bold,color=PurpleEnd,modifier=Modifier.weight(1f));Text("${users.size}",fontWeight=FontWeight.Bold,color=TextMuted,fontSize=12.sp)}}}};if(expanded){items(users,key={"user_${location}_$it"}){user->val gender=vm.userGenders.value[user].orEmpty().ifBlank{"Unknown"};Row(Modifier.fillMaxWidth().padding(start=24.dp,top=6.dp,bottom=6.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("🟢 $user",fontWeight=FontWeight.SemiBold,color=TextDark);Text(gender,color=TextMuted,fontSize=12.sp)};TextButton(onClick={onSelect(user)}){Text("Chat")}}}}}}}},confirmButton={TextButton(onClick=onClose){Text("Close")}})
}

@Composable fun PrivateChatDialog(vm:ChatViewModel,me:String,recipient:String,onClose:()->Unit){
    var text by remember{mutableStateOf("")}; val msgs=vm.privateMessages.mapNotNull{parse(it)}.filter{it.sender==recipient||it.sender==me}
    AlertDialog(onDismissRequest=onClose,title={Text("💬 Private Chat with $recipient")},text={Column(Modifier.fillMaxWidth().height(360.dp)){LazyColumn(Modifier.weight(1f)){items(msgs){m->Bubble(m,m.sender==me)}};Row(verticalAlignment=Alignment.CenterVertically){ImagePickerButton{vm.sendPrivateImage(recipient,it)};OutlinedTextField(text,{text=it},placeholder={Text("Private message...")},singleLine=true,modifier=Modifier.weight(1f));Spacer(Modifier.width(5.dp));Button(onClick={vm.sendPrivateMessage(recipient,text.trim());text=""},enabled=text.isNotBlank(),colors=ButtonDefaults.buttonColors(containerColor=PurpleStart)){Text("Send")}}}},confirmButton={TextButton(onClick=onClose){Text("Close")}})
}
