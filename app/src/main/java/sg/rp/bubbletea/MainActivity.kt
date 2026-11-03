package sg.rp.bubbletea

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

data class Slot(val id:String,val start:String,val end:String,val capacity:Int)
data class Ticket(val studentId:String,val slotId:String,val slotLabel:String,val ticket:String,val timestamp:Long)

class Store(private val context:Context){
  private val p=context.getSharedPreferences("bubble_tea_queue",Context.MODE_PRIVATE)
  fun slots():List<Slot>{ val raw=p.getString("slots",null)?:return defaults(); return try{val a=JSONArray(raw);(0 until a.length()).map{val o=a.getJSONObject(it);Slot(o.getString("id"),o.getString("start"),o.getString("end"),o.getInt("capacity"))}}catch(e:Exception){defaults()} }
  fun saveSlots(v:List<Slot>){val a=JSONArray();v.forEach{a.put(JSONObject().put("id",it.id).put("start",it.start).put("end",it.end).put("capacity",it.capacity))};p.edit().putString("slots",a.toString()).apply()}
  fun tickets():List<Ticket>{val a=JSONArray(p.getString("tickets","[]")!!);return (0 until a.length()).map{val o=a.getJSONObject(it);Ticket(o.getString("studentId"),o.getString("slotId"),o.getString("slotLabel"),o.getString("ticket"),o.getLong("timestamp"))}}
  fun saveTicket(t:Ticket){val a=JSONArray(p.getString("tickets","[]")!!);a.put(JSONObject().put("studentId",t.studentId).put("slotId",t.slotId).put("slotLabel",t.slotLabel).put("ticket",t.ticket).put("timestamp",t.timestamp));p.edit().putString("tickets",a.toString()).apply()}
  fun reset(){p.edit().clear().apply()}
  private fun defaults()=listOf(Slot("S0900","09:00","10:00",30),Slot("S1000","10:00","11:00",30),Slot("S1100","11:00","12:00",30),Slot("S1200","12:00","13:00",30),Slot("S1300","13:00","14:00",30),Slot("S1400","14:00","15:00",30))
}

class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF75412D),secondary=Color(0xFFE58A47),background=Color(0xFFFFF8EF))){App()}}}}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun App(){
  val c=LocalContext.current;val store=remember{Store(c)};var screen by remember{mutableStateOf("home")};var slots by remember{mutableStateOf(store.slots())};var tickets by remember{mutableStateOf(store.tickets())};var selected by remember{mutableStateOf<Slot?>(null)};var scanned by remember{mutableStateOf("")};var issued by remember{mutableStateOf<Ticket?>(null)};var message by remember{mutableStateOf("")}
  fun remaining(s:Slot)=s.capacity-tickets.count{it.slotId==s.id}
  Scaffold(topBar={TopAppBar(title={Text("Free Bubble Tea Queue")},colors=TopAppBarDefaults.topAppBarColors(containerColor=Color(0xFF75412D),titleContentColor=Color.White))}){pad->
   Column(Modifier.padding(pad).padding(14.dp).fillMaxSize()){
    when(screen){
     "home"-> {Text("1. Select collection hour",fontSize=22.sp);Spacer(Modifier.height(10.dp));LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(slots){s->val rem=remaining(s);Card(colors=CardDefaults.cardColors(containerColor=if(rem>0) Color.White else Color(0xFFF2E8E8)),onClick={if(rem>0){selected=s;screen="scan";message=""}},enabled=rem>0){Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween){Column{Text("${s.start} to ${s.end}",fontSize=19.sp);Text("$rem of ${s.capacity} remaining")};Text(if(rem>0)"AVAILABLE" else "NOT AVAILABLE",color=if(rem>0)Color(0xFF17663A)else Color(0xFFA42424))}}}};Spacer(Modifier.height(10.dp));Button(onClick={screen="lookup"},modifier=Modifier.fillMaxWidth()){Text("Check Existing Ticket")};OutlinedButton(onClick={screen="admin"},modifier=Modifier.fillMaxWidth()){Text("Kiosk Settings")}}
     "scan"->{Text("Selected: ${selected!!.start} to ${selected!!.end}",fontSize=20.sp);Spacer(Modifier.height(8.dp));Scanner{value->scanned=value.trim()};Spacer(Modifier.height(8.dp));OutlinedTextField(scanned,{scanned=it},label={Text("Student ID from QR")},modifier=Modifier.fillMaxWidth());if(message.isNotBlank())Text(message,color=Color(0xFFA42424));Button(onClick={val sid=scanned.trim();val ex=tickets.find{it.studentId.equals(sid,true)};val s=selected!!;when{sid.isBlank()->message="Scan or enter a Student ID.";ex!=null->message="Ticket already issued: ${ex.ticket}, ${ex.slotLabel}.";remaining(s)<=0->{message="NOT AVAILABLE. This collection slot is full.";screen="home"};else->{val n=tickets.count{it.slotId==s.id}+1;val t=Ticket(sid,s.id,"${s.start} to ${s.end}","${s.start.substring(0,2)}-${n.toString().padStart(3,'0')}",System.currentTimeMillis());store.saveTicket(t);tickets=store.tickets();issued=t;screen="ticket"}}},modifier=Modifier.fillMaxWidth()){Text("Issue Queue Ticket")};OutlinedButton(onClick={screen="home"},modifier=Modifier.fillMaxWidth()){Text("Back")}}
     "ticket"->{val t=issued!!;Column(Modifier.fillMaxWidth().background(Color(0xFFFFF4E8),RoundedCornerShape(18.dp)).padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("QUEUE TICKET",fontSize=18.sp);Text(t.ticket,fontSize=52.sp,color=Color(0xFF75412D));Text("Collect during",fontSize=16.sp);Text(t.slotLabel,fontSize=28.sp);Text(t.studentId);Spacer(Modifier.height(16.dp));Button(onClick={selected=null;scanned="";issued=null;screen="home"},modifier=Modifier.fillMaxWidth()){Text("Next Student")}}}
     "lookup"->{var q by remember{mutableStateOf("")};Text("Check Existing Ticket",fontSize=22.sp);OutlinedTextField(q,{q=it},label={Text("Student ID")},modifier=Modifier.fillMaxWidth());val f=tickets.find{it.studentId.equals(q.trim(),true)};if(q.isNotBlank())Text(if(f==null)"No ticket found." else "${f.ticket}  |  ${f.slotLabel}",fontSize=24.sp,color=Color(0xFF75412D));Button(onClick={screen="home"},modifier=Modifier.fillMaxWidth()){Text("Back")}}
     "admin"->{Text("Hourly capacities",fontSize=22.sp);LazyColumn(Modifier.weight(1f)){items(slots){s->var v by remember(s.id,s.capacity){mutableStateOf(s.capacity.toString())};Row(verticalAlignment=Alignment.CenterVertically){Text("${s.start}-${s.end}",Modifier.weight(1f));OutlinedTextField(v,{x->v=x.filter{it.isDigit()};val cap=v.toIntOrNull()?.coerceIn(0,999)?:0;slots=slots.map{if(it.id==s.id)it.copy(capacity=cap)else it};store.saveSlots(slots)},label={Text("Capacity")},modifier=Modifier.width(130.dp))}}};Button(onClick={store.reset();slots=store.slots();tickets=store.tickets();screen="home"},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFA42424))){Text("Reset Event Data")};OutlinedButton(onClick={screen="home"},modifier=Modifier.fillMaxWidth()){Text("Back")}}
    }
   }
  }
}

@Composable fun Scanner(onScan:(String)->Unit){
 val c=LocalContext.current;var granted by remember{mutableStateOf(ContextCompat.checkSelfPermission(c,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)};val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted=it};LaunchedEffect(Unit){if(!granted)launcher.launch(Manifest.permission.CAMERA)}
 if(granted) AndroidView(factory={ctx->PreviewView(ctx).also{pv->val future=ProcessCameraProvider.getInstance(ctx);future.addListener({val provider=future.get();val preview=Preview.Builder().build().also{it.surfaceProvider=pv.surfaceProvider};val analyzer=ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();val options=BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build();val scanner=BarcodeScanning.getClient(options);var last="";analyzer.setAnalyzer(Executors.newSingleThreadExecutor()){proxy->val media=proxy.image;if(media==null){proxy.close();return@setAnalyzer};val image=InputImage.fromMediaImage(media,proxy.imageInfo.rotationDegrees);scanner.process(image).addOnSuccessListener{codes->val raw=codes.firstOrNull()?.rawValue?.trim();if(!raw.isNullOrBlank()&&raw!=last){last=raw;onScan(raw)}}.addOnCompleteListener{proxy.close()}};try{provider.unbindAll();provider.bindToLifecycle(ctx as ComponentActivity,CameraSelector.DEFAULT_BACK_CAMERA,preview,analyzer)}catch(_:Exception){}},ContextCompat.getMainExecutor(ctx))}}},modifier=Modifier.fillMaxWidth().height(320.dp)) else Text("Camera permission is required. You can also type the Student ID manually.")
}
