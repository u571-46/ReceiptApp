package com.tama.receiptapp

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.*
import java.net.URLEncoder
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { ReceiptScreen() } } }
  }
}

private const val BUSINESS_NAME = "Tama Ideas Rent"
private const val TARIF_PER_KM = 3500.0
private val BASE_TARIF = mapOf(
  "Drop off" to 100_000.0,
  "Pulang pergi" to 200_000.0,
  "Sewa 6 jam" to 150_000.0,
  "Sewa 12 jam" to 250_000.0,
  "Sewa 18 jam" to 350_000.0,
)
private val JENIS_OPTIONS = listOf("Drop off","Pulang pergi","Sewa 6 jam","Sewa 12 jam","Sewa 18 jam")
private val METODE_BAYAR = listOf("Tunai","Transfer","QRIS")

@Composable
fun ReceiptScreen() {
  val ctx = androidx.compose.ui.platform.LocalContext.current
  var customer by remember { mutableStateOf("") }
  var driver by remember { mutableStateOf("") }
  var origin by remember { mutableStateOf("") }
  var destination by remember { mutableStateOf("") }
  var jenis by remember { mutableStateOf(JENIS_OPTIONS.first()) }
  var metode by remember { mutableStateOf(METODE_BAYAR.first()) }
  var kmTxt by remember { mutableStateOf("") }
  var paid by remember { mutableStateOf(false) }
  var waNumber by remember { mutableStateOf("62812XXXXXXX") }

  val km = kmTxt.toDoubleOrNull() ?: 0.0
  val base = BASE_TARIF[jenis] ?: 0.0
  val total = base + km * TARIF_PER_KM

  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
    Box(
      modifier = Modifier.fillMaxWidth()
        .background(Brush.horizontalGradient(listOf(Color(0xFF0D47A1), Color(0xFF42A5F5))), RoundedCornerShape(12.dp))
        .padding(16.dp)
    ) { Text("$BUSINESS_NAME — Receipt", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }

    Spacer(Modifier.height(12.dp))
    OutlinedTextField(customer, { customer = it }, label = { Text("Nama Pelanggan") }, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(driver, { driver = it }, label = { Text("Nama Pengemudi") }, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(origin, { origin = it }, label = { Text("Asal") }, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(destination, { destination = it }, label = { Text("Tujuan") }, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    Dropdown("Jenis Perjalanan", JENIS_OPTIONS, jenis) { jenis = it }
    Spacer(Modifier.height(8.dp))
    Dropdown("Metode Pembayaran", METODE_BAYAR, metode) { metode = it }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
      kmTxt, { t -> if (t.isEmpty() || t.matches(Regex("^\\d*(\\.\\d*)?$"))) kmTxt = t },
      label = { Text("Jarak (km)") },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      Checkbox(checked = paid, onCheckedChange = { paid = it })
      Text("Tandai LUNAS")
    }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(waNumber, { waNumber = it }, label = { Text("Nomor WhatsApp Tujuan (62...)") }, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(16.dp))
