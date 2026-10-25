package online.nexalink.app.ui.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import online.nexalink.app.R
import online.nexalink.app.extension.toastSuccess
import online.nexalink.app.handler.NexalinkAccountManager
import online.nexalink.app.util.QRCodeDecoder

/** NEXALINK: приглашение друзей и доступ для другого устройства — общие шторки для главного, меню и «Моего доступа». */
object ShareLinks {
    const val SITE = "https://nexalink.online"
    const val APK = "https://nexalink.online/dl/nexalink.apk"
    const val INVITE_TEXT =
        "NEXALINK — бесплатный VPN без регистрации и рекламы 🛰\nAndroid: $APK\niPhone и компьютер: $SITE"

    fun share(context: Context, text: String, title: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun copy(context: Context, text: String) {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("NEXALINK", text))
        context.toastSuccess("Скопировано")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var showQr by remember { mutableStateOf(false) }
    val qr = remember(showQr) { if (showQr) QRCodeDecoder.createQRCode(ShareLinks.SITE, 600) else null }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding()
            .verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(R.drawable.nexalink_logo), null, Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)))
                Column {
                    Text("Пригласить друзей", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("NEXALINK бесплатный для всех", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            SectionLabel("Друг получит")
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Text(ShareLinks.INVITE_TEXT, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(14.dp))
            }
            if (qr != null) {
                Spacer(Modifier.height(14.dp))
                QrImage(qr, caption = "Наведите камеру — откроется nexalink.online")
            }
            Spacer(Modifier.height(16.dp))
            PillButton("Отправить приглашение") { ShareLinks.share(context, ShareLinks.INVITE_TEXT, "Пригласить в NEXALINK") }
            Spacer(Modifier.height(8.dp))
            PillOutlined(if (showQr) "Скрыть QR-код" else "Показать QR-код сайта") { showQr = !showQr }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherDeviceSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    fun load(forceNew: Boolean) {
        loading = true; error = null
        scope.launch {
            val (u, e) = withContext(Dispatchers.IO) { NexalinkAccountManager.getExtraDeviceLink(forceNew) }
            url = u; error = e; loading = false
        }
    }
    LaunchedEffect(Unit) { load(false) }
    val qr = remember(url) { url?.let { QRCodeDecoder.createQRCode(it, 600) } }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding()
            .verticalScroll(rememberScrollState())) {
            Text("Доступ для другого устройства", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Отдельная бесплатная ссылка — свою делить не нужно.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            when {
                loading -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                error != null -> {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    PillButton("Попробовать ещё раз") { load(false) }
                }
                url != null -> {
                    qr?.let { QrImage(it, caption = null) }
                    Spacer(Modifier.height(14.dp))
                    Step(1, "На iPhone или компьютере установите Happ")
                    Step(2, "В Happ: «+» → «Сканировать QR-код»")
                    Step(3, "Включите режим «Туннель» и подключитесь")
                    Spacer(Modifier.height(12.dp))
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                        Text(url!!, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { PillButton("Копировать") { ShareLinks.copy(context, url!!) } }
                        Box(Modifier.weight(1f)) {
                            PillOutlined("Отправить") {
                                ShareLinks.share(context, "Доступ к NEXALINK — добавьте ссылку в Happ («+» → «Вставить из буфера»):\n${url!!}",
                                    "Отправить доступ")
                            }
                        }
                    }
                    TextButton(onClick = { load(true) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Выдать новую ссылку", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun Step(n: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(vertical = 3.dp)) {
        Box(Modifier.size(22.dp).clip(RoundedCornerShape(11.dp)).background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center) {
            Text("$n", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun QrImage(bitmap: Bitmap, caption: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Surface(shape = RoundedCornerShape(18.dp), color = androidx.compose.ui.graphics.Color.White,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Image(bitmap.asImageBitmap(), contentDescription = "QR-код", modifier = Modifier.size(200.dp).padding(10.dp))
        }
        if (caption != null) {
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun PillButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, shape = RoundedCornerShape(99.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PillOutlined(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, shape = RoundedCornerShape(99.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) {
        Text(text, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
