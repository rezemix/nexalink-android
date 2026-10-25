package online.nexalink.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import online.nexalink.app.R
import online.nexalink.app.dto.entities.ServersCache
import online.nexalink.app.ui.compose.AppDivider
import online.nexalink.app.ui.compose.colorPing
import online.nexalink.app.ui.compose.colorPingRed

/**
 * Простой список серверов «как у крупных VPN-сервисов»: флаг + название + пинг,
 * тап — выбор и закрытие. Полное управление конфигами (редактирование,
 * экспорт, дедупликация и т.д.) осталось в «Подписки» в боковом меню — сюда
 * вынесен только повседневный сценарий «выбрать сервер».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerPickerSheet(
    sheetState: SheetState,
    servers: List<ServersCache>,
    selectedGuid: String?,
    isTesting: Boolean,
    isAutoMode: Boolean,
    onSelect: (String) -> Unit,
    onSetAutoMode: () -> Unit,
    onTestAll: () -> Unit,
    onImportAction: (MainAction) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResourceServerPickerTitle(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Добавить сервер вручную (QR / буфер обмена / файл) — например,
                    // WireGuard-конфиг домашнего Pi4-релея для обхода блокировок.
                    var showImportMenu by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                        IconButton(onClick = { showImportMenu = true }) {
                            Icon(painter = painterResource(R.drawable.ic_add_24dp), contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = showImportMenu,
                            onDismissRequest = { showImportMenu = false },
                        ) {
                            ImportMenuContent(
                                onAction = { action ->
                                    showImportMenu = false
                                    onImportAction(action)
                                }
                            )
                        }
                    }
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = onTestAll) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check_update_24dp),
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
            AppDivider()

            if (servers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Нет доступных серверов",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                // NEXALINK: подпись у "Авто" должна отражать реально самый быстрый
                // сервер по текущим пингам в этом списке, а не тот, что был выбран
                // при последнем подключении — иначе после ре-теста пингов подпись
                // расходится с цифрами (например, показывает Russia 2 с 107мс, хотя
                // Finland рядом висит 86мс). Та же логика выбора, что и при реальном
                // авто-подключении в MainViewModel.selectBestServerIfAuto().
                val autoServerName = servers
                    .filter { it.testDelayString.isNotEmpty() && it.testDelayMillis >= 0L }
                    .minByOrNull { it.testDelayMillis }
                    ?.profile?.remarks
                    ?: servers.firstOrNull { it.guid == selectedGuid }?.profile?.remarks
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    item(key = "auto") {
                        AutoServerRow(
                            isSelected = isAutoMode,
                            resolvedServerName = autoServerName,
                            onClick = onSetAutoMode,
                        )
                    }
                    item(key = "auto-divider") { AppDivider() }
                    items(servers, key = { it.guid }) { server ->
                        ServerPickerRow(
                            server = server,
                            isSelected = !isAutoMode && server.guid == selectedGuid,
                            onClick = { onSelect(server.guid) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun stringResourceServerPickerTitle(): String = "Выбор сервера"

/**
 * Закреплённый первый пункт: приложение само выбирает самый быстрый по
 * последнему пингу сервер. Рекомендуемый режим для обычных пользователей —
 * ручной выбор ниже остаётся доступным, но уже не обязателен.
 */
@Composable
private fun AutoServerRow(
    isSelected: Boolean,
    resolvedServerName: String?,
    onClick: () -> Unit,
) {
    PickerRowFrame(isSelected = isSelected, onClick = onClick) {
        FlagCircle("⚡")
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Авто",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = resolvedServerName?.let { "Сейчас: ${serverDisplayName(it)}" } ?: "Самый быстрый сервер",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isSelected) SelectedCheck()
    }
}

/** Строка шторки: выбранная — на сером фоне со скруглением, как в макете. */
@Composable
private fun PickerRowFrame(isSelected: Boolean, onClick: () -> Unit, tint: Color = Color.Transparent, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.surfaceContainer else tint)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun FlagCircle(text: String, background: Color = MaterialTheme.colorScheme.surfaceContainer) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center,
    ) { Text(text = text, style = MaterialTheme.typography.titleMedium) }
}

@Composable
private fun SelectedCheck() {
    Box(
        modifier = Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = Icons.Filled.Check, contentDescription = "Выбран",
            tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun ServerPickerRow(
    server: ServersCache,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val isEmergency = isEmergencyServerName(server.profile.remarks)
    PickerRowFrame(isSelected = isSelected, onClick = onClick,
        tint = if (isEmergency) colorPingRed.copy(alpha = 0.08f) else Color.Transparent) {
        FlagCircle(
            flagForServerName(server.profile.remarks),
            if (isEmergency) colorPingRed.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainer,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = serverDisplayName(server.profile.remarks),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isEmergency) colorPingRed else Color.Unspecified,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = when {
                isEmergency -> "Резервный канал через домашний интернет"
                server.testDelayString.isNotEmpty() && server.testDelayMillis < 0L -> "Не отвечает"
                server.testDelayString.isNotEmpty() -> server.testDelayString
                else -> null
            }
            if (sub != null) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isEmergency || server.testDelayMillis < 0L -> colorPingRed
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (isSelected) SelectedCheck()
    }
}

/**
 * NEXALINK: сервер-резерв "на случай БО" (домашний релей через Reality-chain,
 * см. frontend/dl/pi4-relay-setup.sh) помечается маркером 🆘 в названии —
 * по этому маркеру подсвечиваем его красным, чтобы визуально отличать от
 * обычных серверов.
 */
fun isEmergencyServerName(remarks: String): Boolean =
    remarks.contains("🆘") || remarks.contains("Аварийный", ignoreCase = true)

/**
 * Название без флагов в начале ("🇷🇺→🇫🇮 Москва" → "Москва → Финляндия", "🇫🇮 Финляндия 1" → "Финляндия 1") —
 * флаг и так в кружке слева.
 */
fun serverDisplayName(remarks: String): String {
    val flagRe = Regex("^[\\s\\x{1F1E6}-\\x{1F1FF}→\\-]+")
    val head = flagRe.find(remarks)?.value.orEmpty()
    val name = remarks.removePrefix(head).trim().ifEmpty { remarks }
    // цепочка «вход → выход»: показываем, куда в итоге идёт трафик
    if ("→" in head) {
        val flags = Regex("[\\x{1F1E6}-\\x{1F1FF}]{2}").findAll(head).map { it.value }.toList()
        val exit = when (flags.lastOrNull()) {
            "🇫🇮" -> "Финляндия"; "🇩🇪" -> "Германия"; "🇳🇱" -> "Нидерланды"; else -> null
        }
        if (exit != null && !name.contains(exit)) return "$name → $exit"
    }
    return name
}

/** Грубое сопоставление названия сервера со страной по ключевым словам в remarks. */
fun flagForServerName(remarks: String): String {
    // флаг уже в начале названия (так их отдаёт панель) — берём первый
    Regex("^\\s*([\\x{1F1E6}-\\x{1F1FF}]{2})").find(remarks)?.let { return it.groupValues[1] }
    val name = remarks.lowercase()
    return when {
        "russia" in name || "росси" in name -> "🇷🇺"
        "finland" in name || "финлянд" in name -> "🇫🇮"
        "german" in name || "герман" in name -> "🇩🇪"
        "netherlands" in name || "нидерланд" in name -> "🇳🇱"
        "united states" in name || "usa" in name || "сша" in name -> "🇺🇸"
        "united kingdom" in name || "uk" in name -> "🇬🇧"
        "france" in name || "франц" in name -> "🇫🇷"
        "turkey" in name || "турци" in name -> "🇹🇷"
        "japan" in name || "япон" in name -> "🇯🇵"
        "singapore" in name || "сингапур" in name -> "🇸🇬"
        "poland" in name || "польш" in name -> "🇵🇱"
        else -> "🌐"
    }
}
