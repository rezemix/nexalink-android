package online.nexalink.app.ui.account

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import online.nexalink.app.handler.SettingsManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Switch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import online.nexalink.app.extension.toastSuccess
import online.nexalink.app.ui.base.BaseComponentActivity
import online.nexalink.app.ui.compose.AppTopBar

class NexalinkAccountActivity : BaseComponentActivity() {

    private val viewModel: NexalinkAccountViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun ScreenContent() {
        MyAccessScreen(viewModel = viewModel, onBackClick = { finish() })
    }
}

@Composable
fun MyAccessScreen(
    viewModel: NexalinkAccountViewModel,
    onBackClick: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val subUrl by viewModel.subscriptionUrl.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        topBar = { AppTopBar(title = "Мой доступ", onBackClick = onBackClick, isLoading = isLoading) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val url = subUrl
            if (url != null) {
                AccessContent(url)
            } else {
                NoAccessContent(isLoading, errorMessage, onGetAccess = { viewModel.getFreeAccess() })
            }
        }
    }
}

@Composable
private fun AccessContent(url: String) {
    val context = LocalContext.current
    var showOtherDevice by remember { mutableStateOf(false) }
    var showInvite by remember { mutableStateOf(false) }
    if (showOtherDevice) online.nexalink.app.ui.share.OtherDeviceSheet(onDismiss = { showOtherDevice = false })
    if (showInvite) online.nexalink.app.ui.share.InviteSheet(onDismiss = { showInvite = false })

    SectionTitle("Этот телефон")
    AccessCard {
        Text("ССЫЛКА ДОСТУПА", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            SelectionContainer {
                Text(url, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp))
            }
        }
        Button(
            onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("NEXALINK", url))
                context.toastSuccess("Ссылка скопирована")
            },
            shape = RoundedCornerShape(99.dp),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Копировать") }
        // своя ссылка — только для этого телефона; другим — отдельный доступ ниже
        Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text(
                "Не делитесь этой ссылкой — она только для этого телефона. Для друзей и других устройств — ниже.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
    SectionTitle("Другие устройства")
    ActionRow("📱", MaterialTheme.colorScheme.primary, "Доступ для другого устройства",
        "iPhone, компьютер, планшет — по QR-коду") { showOtherDevice = true }
    ActionRow("💌", MaterialTheme.colorScheme.secondary, "Пригласить друзей",
        "Отправить ссылку на NEXALINK") { showInvite = true }
    SectionTitle("Настройки")
    RoutingToggles()
}

@Composable
private fun SectionTitle(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun ActionRow(emoji: String, tint: androidx.compose.ui.graphics.Color, title: String, hint: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) { Text(emoji, style = MaterialTheme.typography.titleMedium) }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AccessCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) { Column(modifier = Modifier.padding(16.dp), content = content) }
}

/** Блокировка рекламы и «российские сайты напрямую» — правила маршрутизации; VPN переподключится на главном. */
@Composable
private fun RoutingToggles() {
    var ads by remember { mutableStateOf(SettingsManager.isAdBlockEnabled()) }
    var ru by remember { mutableStateOf(SettingsManager.isRuDirectEnabled()) }
    AccessCard {
        ToggleRow("Блокировать рекламу", "Рекламные сети и трекеры", ads) { ads = it; SettingsManager.setAdBlock(it) }
        ToggleRow("Российские сайты — без VPN", "Сайты и приложения банков, Госуслуг — напрямую", ru) { ru = it; SettingsManager.setRuDirect(it) }
    }
}

@Composable
private fun ToggleRow(title: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.tertiary)
        )
    }
}

@Composable
private fun NoAccessContent(isLoading: Boolean, errorMessage: String?, onGetAccess: () -> Unit) {
    Text("Доступа пока нет", style = MaterialTheme.typography.titleLarge)
    Text(
        "NEXALINK бесплатный: без регистрации, оплаты и лимитов трафика.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    if (errorMessage != null) {
        Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    Button(onClick = onGetAccess, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(vertical = 2.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text("Получить бесплатный доступ")
        }
    }
}
