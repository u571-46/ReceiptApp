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

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Button(Modifier.weight(1f), onClick = {
        val bytes = createReceiptPdfBytes(customer, driver, origin, destination, jenis, metode, km, base, total, paid)
        if (bytes != null && savePdfToDownloads(ctx, "receipt_${System.currentTimeMillis()}.pdf", bytes))
          toast(ctx, "Disimpan ke Downloads")
        else toast(ctx, "Gagal simpan PDF")
      }) { Text("Simpan PDF") }

      Button(Modifier.weight(1f), onClick = {
        val bytes = createReceiptPdfBytes(customer, driver, origin, destination, jenis, metode, km, base, total, paid)
        val file = bytes?.let { writeTempPdf(ctx, it) } ?: return@Button
        shareViaWhatsApp(ctx, file, customer, total, waNumber)
      }) { Text("Kirim via WA") }
    }
  }
}

@Composable
private fun Dropdown(label:String, options: List<String>, selected: String, onSelect:(String)->Unit) {
  var expanded by remember { mutableStateOf(false) }
  Column {
    Text(label, fontWeight = FontWeight.Medium)
    Box(Modifier.background(Color.White, RoundedCornerShape(8.dp)).clickable { expanded = true }.padding(12.dp).fillMaxWidth()) { Text(selected) }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { DropdownMenuItem(onClick = { onSelect(it); expanded = false }) { Text(it) } }
    }
  }
}

private fun toast(ctx: Context, msg:String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
private fun formatRupiah(v: Double) = NumberFormat.getCurrencyInstance(Locale("id","ID")).format(v)

private fun createReceiptPdfBytes(
  customer:String, driver:String, origin:String, destination:String, jenis:String,
  metode:String, km:Double, base:Double, total:Double, paid:Boolean
): ByteArray? = try {
  val pageWidth = 595; val pageHeight = 842
  val doc = PdfDocument(); val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
  val page = doc.startPage(info); val c = page.canvas; val p = Paint(Paint.ANTI_ALIAS_FLAG)
  var y = 50f
  p.textSize = 14f; p.isFakeBoldText = true; c.drawText("Tama Ideas Rent", 40f, y, p)
  y += 30f; p.isFakeBoldText = false
  c.drawText("Nama Pelanggan: $customer", 40f, y, p); y += 20f
  c.drawText("Nama Pengemudi: $driver", 40f, y, p); y += 20f
  c.drawText("Asal: $origin", 40f, y, p); y += 20f
  c.drawText("Tujuan: $destination", 40f, y, p); y += 20f
  c.drawText("Jenis: $jenis", 40f, y, p); y += 20f
  c.drawText("Metode Pembayaran: $metode", 40f, y, p); y += 20f
  c.drawText("Jarak: $km km", 40f, y, p); y += 20f
  c.drawText("Tarif dasar: ${formatRupiah(base)}", 40f, y, p); y += 20f
  c.drawText("Total: ${formatRupiah(total)}", 40f, y, p); y += 30f
  if (paid) { p.color = Color.GREEN; p.textSize = 24f; c.drawText("LUNAS", 250f, y, p) }
  doc.finishPage(page)
  val bos = ByteArrayOutputStream(); doc.writeTo(bos); doc.close(); bos.toByteArray()
} catch (e: Exception) { null }

private fun savePdfToDownloads(context: Context, name:String, data:ByteArray): Boolean = try {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    val v = ContentValues().apply {
      put(MediaStore.Downloads.DISPLAY_NAME, name)
      put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
      put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ReceiptApp")
      put(MediaStore.Downloads.IS_PENDING, 1)
    }
    val r = context.contentResolver
    val uri = r.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v) ?: return false
    r.openOutputStream(uri)?.use { it.write(data) }
    v.clear(); v.put(MediaStore.Downloads.IS_PENDING, 0); r.update(uri, v, null, null)
    true
  } else false
} catch (e: Exception) { false }

private fun writeTempPdf(context: Context, data: ByteArray): File? = try {
  File(context.getExternalFilesDir(null), "share_${System.currentTimeMillis()}.pdf").apply { FileOutputStream(this).use { it.write(data) } }
} catch (e: Exception) { null }

private fun shareViaWhatsApp(context: Context, file: File, customer:String, total:Double, phone:String) {
  val uri = androidx.core.content.FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
  val message = "Invoice ${customer}\nTotal: ${formatRupiah(total)}"
  val url = "https://wa.me/$phone?text=" + URLEncoder.encode(message, "UTF-8")
  try {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
  } catch (_: Exception) {}
  val sendIntent = Intent(Intent.ACTION_SEND).apply {
    type = "application/pdf"; setPackage("com.whatsapp"); putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  try { context.startActivity(sendIntent) } catch (_: Exception) { Toast.makeText(context, "Gagal buka WhatsApp", Toast.LENGTH_SHORT).show() }
}
