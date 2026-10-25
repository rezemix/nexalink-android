package online.nexalink.app.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import online.nexalink.app.R
import online.nexalink.app.dto.entities.ServersCache
import online.nexalink.app.handler.MmkvManager
import online.nexalink.app.ui.compose.colorConnected
import online.nexalink.app.ui.compose.colorConnecting
import online.nexalink.app.ui.compose.colorPingRed
import online.nexalink.app.handler.ApkUpdateInstaller
import online.nexalink.app.util.Utils
import online.nexalink.app.handler.UpdateDownloadState

/**
 * Главный экран — простой и дружелюбный, как у крупных VPN-сервисов: одна
 * большая кнопка подключения и карточка выбора сервера. Управление
 * конфигами/подписками/маршрутизацией по-прежнему доступно через боковое
 * меню — здесь только повседневный сценарий «подключиться».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val isRunning = uiState.isRunning
    val isTesting = uiState.isTesting
    val statusText = uiState.statusText
    val selectedGuid = uiState.selectedGuid

    val activeGroupId = uiState.selectedGroupId.ifEmpty { uiState.groups.firstOrNull()?.id }
    val servers: List<ServersCache> by (
        if (activeGroupId != null) mainViewModel.serversForGroup(activeGroupId)
        else remember { kotlinx.coroutines.flow.MutableStateFlow(emptyList()) }
        ).collectAsStateWithLifecycle()

    val currentServerName = remember(selectedGuid) {
        selectedGuid?.let { MmkvManager.decodeServerConfig(it)?.remarks }
    } ?: "Сервер не выбран"

    // Автоматически пингуем серверы при первом появлении списка (как в
    // крупных VPN-сервисах — не нужно нажимать отдельную кнопку).
    // Используем "настоящий" тест через реальный протокол (TestRealAllServers),
    // а не голый TCP-коннект — обычный TCP-пинг к VLESS/Reality-серверам часто
    // режется DPI провайдера и показывает -1мс даже на рабочих серверах.
    LaunchedEffect(activeGroupId, servers.size) {
        if (activeGroupId != null && servers.isNotEmpty() && !isTesting &&
            servers.all { it.testDelayString.isEmpty() }
        ) {
            onAction(MainAction.TestRealAllServers)
        }
    }

    // приветствие с получением доступа — пока на устройстве нет ни одного сервера
    val showWelcome = uiState.needsAccess && servers.isEmpty()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showServerPicker by remember { mutableStateOf(false) }
    var showInvite by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MainDrawerContent(
                drawerState = drawerState,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    if (route == MainDestination.Invite) showInvite = true else onNavigate(route)
                }
            )
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                // на экране первого запуска шапки нет — как в макете
                if (!showWelcome) {
                    HomeTopBar(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onAccountClick = { onNavigate(MainDestination.NexalinkAccount) },
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val daysLeft = uiState.subscriptionDaysLeft
                if (daysLeft != null) {
                    SubscriptionExpiryBanner(
                        daysLeft = daysLeft,
                        onDismiss = { onAction(MainAction.DismissSubscriptionBanner) },
                    )
                }
                if (showWelcome) {
                    FreeAccessContent(
                        isLoading = uiState.freeAccessLoading,
                        error = uiState.freeAccessError,
                        onGetAccess = { onAction(MainAction.GetFreeAccess) },
                        onPasteLink = { onAction(MainAction.ImportClipboard) },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ConnectButton(
                                isRunning = isRunning,
                                onClick = { onAction(MainAction.ToggleService) },
                            )
                            Spacer(modifier = Modifier.height(if (isRunning) 18.dp else 26.dp))
                            Text(
                                text = if (isRunning) "Подключено" else "Не подключено",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isRunning) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (isRunning) ConnectionTimer(uiState.sessionStartMs) else Text(
                                text = "Нажмите, чтобы включить",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            // результат проверки соединения (нажатие на статус в старых версиях) — если есть
                            if (isRunning && statusText.isNotBlank() && !statusText.startsWith("Соединено")) {
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (isRunning) {
                                Spacer(modifier = Modifier.height(14.dp))
                                StatusPills(adBlock = uiState.adBlockOn, ruDirect = uiState.ruDirectOn)
                            }
                        }
                    }
                    if (isRunning) {
                        LiveStatsCards(uiState)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    val selected = servers.firstOrNull { it.guid == selectedGuid }
                    // сервер и IP выхода — одной карточкой; страна — по серверу (у цепочки — страна выхода)
                    ServerSelectorCard(
                        serverName = currentServerName,
                        isAutoMode = uiState.isAutoMode,
                        ping = selected?.takeIf { it.testDelayMillis > 0L }?.testDelayString,
                        exitIp = if (isRunning) uiState.ip else null,
                        exitCountry = if (isRunning) exitCountryCode(currentServerName) ?: uiState.ipCountry else null,
                        isRunning = isRunning,
                        onClick = { showServerPicker = true },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ShareButton(onClick = { showInvite = true })
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showInvite) online.nexalink.app.ui.share.InviteSheet(onDismiss = { showInvite = false })

    if (showServerPicker) {
        ServerPickerSheet(
            sheetState = sheetState,
            servers = servers,
            selectedGuid = selectedGuid,
            isTesting = isTesting,
            isAutoMode = uiState.isAutoMode,
            onSelect = { guid ->
                onAction(MainAction.SelectServer(guid))
                showServerPicker = false
            },
            onSetAutoMode = {
                onAction(MainAction.SetAutoMode(true))
                showServerPicker = false
            },
            onTestAll = { onAction(MainAction.TestRealAllServers) },
            onImportAction = onAction,
            onDismiss = { showServerPicker = false },
        )
    }

    // NEXALINK: автопроверка обновления при запуске — не нужно самому
    // заходить в "Проверка обновлений", всплывает само при наличии новой версии.
    val availableUpdate = uiState.availableUpdate
    if (availableUpdate != null) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { onAction(MainAction.DismissUpdateDialog) },
            title = { Text("Доступна версия ${availableUpdate.latestVersion}") },
            text = {
                Text(
                    availableUpdate.releaseNotes?.takeIf { it.isNotBlank() }
                        ?: "Рекомендуем обновиться."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onAction(MainAction.DismissUpdateDialog)
                    availableUpdate.downloadUrl?.let { url ->
                        ApkUpdateInstaller.downloadAndInstall(
                            context,
                            url,
                            availableUpdate.latestVersion ?: "new"
                        )
                    }
                }) {
                    Text("Обновить")
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(MainAction.DismissUpdateDialog) }) {
                    Text("Позже")
                }
            },
        )
    }

    // NEXALINK: редкое уведомление с сайта (например, "тестируем на боевых нодах") —
    // показывается один раз на id объявления, дальше не повторяется.
    val activeAnnouncement = uiState.activeAnnouncement
    if (activeAnnouncement != null) {
        AnnouncementDialog(
            type = activeAnnouncement.type,
            title = activeAnnouncement.title,
            body = activeAnnouncement.body,
            onDismiss = { onAction(MainAction.DismissAnnouncementDialog) },
        )
    }

    // NEXALINK: видимый прогресс скачивания/установки обновления — раньше
    // было непонятно, грузится оно или зависло. Диалог без кнопки закрытия
    // во время скачивания/установки, чтобы не выглядело как "можно нажать
    // и всё исчезнет, а обновление всё равно идёт".
    val downloadState by ApkUpdateInstaller.downloadState.collectAsStateWithLifecycle()
    when (val state = downloadState) {
        is UpdateDownloadState.Downloading -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Скачивание обновления…") },
                text = {
                    Column {
                        LinearProgressIndicator(
                            progress = { state.progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("${state.progressPercent}%")
                    }
                },
                confirmButton = {},
            )
        }
        UpdateDownloadState.Installing -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Установка…") },
                text = { Text("Открываем установщик, подтвердите установку.") },
                confirmButton = {},
            )
        }
        is UpdateDownloadState.Failed -> {
            AlertDialog(
                onDismissRequest = { ApkUpdateInstaller.dismiss() },
                title = { Text("Не получилось обновить") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = { ApkUpdateInstaller.dismiss() }) {
                        Text("ОК")
                    }
                },
            )
        }
        UpdateDownloadState.Idle -> {}
    }
}

@Composable
private fun HomeTopBar(
    onMenuClick: () -> Unit,
    onAccountClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(painter = painterResource(R.drawable.ic_menu_24dp), contentDescription = "Меню")
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.nexalink_logo),
                contentDescription = null,
                modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)),
            )
            Text(
                text = "NEXALINK",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = androidx.compose.ui.unit.TextUnit(0.6f, androidx.compose.ui.unit.TextUnitType.Sp),
            )
        }
        IconButton(onClick = onAccountClick) {
            Icon(painter = painterResource(R.drawable.ic_key_24dp), contentDescription = "Мой доступ")
        }
    }
}

/** Кнопка питания как на сайте: тёмно-синяя → зелёная с мягким ореолом, когда подключено. */
@Composable
private fun ConnectButton(
    isRunning: Boolean,
    onClick: () -> Unit,
) {
    val color by animateColorAsState(
        targetValue = if (isRunning) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        label = "connectButtonColor",
    )
    val ring by animateColorAsState(
        targetValue = if (isRunning) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent,
        label = "connectRing",
    )
    val iconTint = if (isRunning) Color.White else MaterialTheme.colorScheme.onPrimary
    val outer by androidx.compose.animation.core.animateDpAsState(if (isRunning) 156.dp else 196.dp, label = "outer")
    val inner by androidx.compose.animation.core.animateDpAsState(if (isRunning) 128.dp else 164.dp, label = "inner")
    Box(
        modifier = Modifier
            .size(outer)
            .clip(CircleShape)
            .background(ring),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .size(inner)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            color = color,
            shadowElevation = 10.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_power_24dp),
                    contentDescription = if (isRunning) "Отключить" else "Подключить",
                    tint = iconTint,
                    modifier = Modifier.size(if (isRunning) 48.dp else 60.dp),
                )
            }
        }
    }
}

/** Время с момента подключения (с момента, как экран увидел «Подключено»). */
@Composable
private fun ConnectionTimer(sessionStartMs: Long) {
    // время начала — из процесса VPN, чтобы не сбрасывалось при возврате в приложение
    val fallback = remember { System.currentTimeMillis() }
    val start = if (sessionStartMs > 0) sessionStartMs else fallback
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            now = System.currentTimeMillis()
        }
    }
    val s = (now - start) / 1000
    Text(
        text = "%02d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun StatusPills(adBlock: Boolean, ruDirect: Boolean) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (adBlock) StatusPill("Реклама блокируется")
        if (ruDirect) StatusPill("Российские сайты — без VPN")
    }
}

/** «Скорость» (с графиком последних ~40 с) и «Трафик» — из TrafficStatsHub процесса VPN. */
@Composable
private fun LiveStatsCards(state: MainUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        StatCard(title = "Скорость", modifier = Modifier.weight(1f)) {
            Text("↓ " + speedText(state.downBps), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("↑ " + speedText(state.upBps), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SpeedBars(state.speedHistory)
        }
        StatCard(title = "Трафик", modifier = Modifier.weight(1f)) {
            Text(bytesText(state.sessionBytes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("за подключение", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Сегодня: " + bytesText(state.todayBytes), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StatCard(title: String, modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.height(104.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.background,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun SpeedBars(history: List<Long>) {
    val max = (history.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val color = MaterialTheme.colorScheme.tertiary
    Row(
        modifier = Modifier.fillMaxWidth().height(22.dp).padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(SPEED_BARS) { i ->
            val v = history.getOrNull(history.size - SPEED_BARS + i) ?: 0L
            Box(
                modifier = Modifier.weight(1f)
                    .fillMaxHeight((v.toFloat() / max).coerceIn(0.08f, 1f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(color.copy(alpha = 0.45f))
            )
        }
    }
}

private const val SPEED_BARS = 14

/** Код страны выхода по названию сервера: «🇷🇺→🇫🇮 Москва» → FI, «🇩🇪 Германия» → DE. */
private fun exitCountryCode(remarks: String): String? {
    val flags = Regex("[\\x{1F1E6}-\\x{1F1FF}]{2}").findAll(remarks).map { it.value }.toList()
    val flag = (if ("→" in remarks) flags.lastOrNull() else flags.firstOrNull()) ?: return null
    val cps = flag.codePoints().toArray()
    return cps.joinToString("") { (('A'.code + it - 0x1F1E6).toChar()).toString() }
}

private fun countryLabel(code: String?): Pair<String, String> {
    val cc = code?.uppercase()?.takeIf { it.length == 2 && it.all { c -> c in 'A'..'Z' } } ?: return "🌐" to (code ?: "")
    val flag = cc.map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
    val name = java.util.Locale("", cc).getDisplayCountry(java.util.Locale("ru")).ifEmpty { cc }
    return flag to name
}

private fun speedText(bps: Long): String {
    val bits = bps * 8.0
    return when {
        bits >= 1_000_000 -> String.format(java.util.Locale("ru"), "%.1f Мбит/с", bits / 1_000_000)
        else -> String.format(java.util.Locale("ru"), "%.0f Кбит/с", bits / 1_000)
    }
}

private fun bytesText(b: Long): String = when {
    b >= 1L shl 30 -> String.format(java.util.Locale("ru"), "%.1f ГБ", b / (1L shl 30).toDouble())
    b >= 1L shl 20 -> String.format(java.util.Locale("ru"), "%.0f МБ", b / (1L shl 20).toDouble())
    else -> String.format(java.util.Locale("ru"), "%.0f КБ", b / 1024.0)
}

@Composable
private fun StatusPill(text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
        Text(text = text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * NEXALINK: напоминание об окончании подписки прямо в приложении (раньше
 * было только письмо на почту). Не блокирует ничего — просто баннер сверху,
 * закрывается крестиком до следующего запуска приложения.
 */
@Composable
private fun SubscriptionExpiryBanner(
    daysLeft: Int,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val expired = daysLeft < 0
    val text = when {
        expired -> "Подписка закончилась"
        daysLeft == 0 -> "Подписка заканчивается сегодня"
        else -> "Подписка заканчивается через $daysLeft ${daysWord(daysLeft)}"
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(16.dp)),
        color = colorPingRed.copy(alpha = if (expired) 0.16f else 0.1f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "⏳", style = MaterialTheme.typography.titleMedium)
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { Utils.openUri(context, "https://nexalink.online/cabinet") }) {
                Text("Продлить")
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Скрыть")
            }
        }
    }
}

private fun daysWord(n: Int): String {
    val rem100 = n % 100
    if (rem100 in 11..19) return "дней"
    return when (n % 10) {
        1 -> "день"
        2, 3, 4 -> "дня"
        else -> "дней"
    }
}

/**
 * NEXALINK бесплатный: первый запуск, подписок ещё нет. Доступ запрашивается сам
 * (см. MainViewModel.initialize), здесь — ожидание, ошибка и повтор вручную.
 */
@Composable
private fun FreeAccessContent(
    isLoading: Boolean,
    error: String?,
    onGetAccess: () -> Unit,
    onPasteLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().statusBarsPadding()) {
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.nexalink_logo),
                contentDescription = null,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)),
            )
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "НЕКОММЕРЧЕСКИЙ ПРОЕКТ",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = androidx.compose.ui.unit.TextUnit(1.4f, androidx.compose.ui.unit.TextUnitType.Sp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "VPN, который обходит блокировки",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isLoading) "Получаем для вас личный доступ…"
                else "Бесплатно, без регистрации и рекламы. Российские сайты работают напрямую.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (error != null && !isLoading) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        }
        Button(
            onClick = onGetAccess,
            enabled = !isLoading,
            shape = RoundedCornerShape(99.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Получить доступ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        Text(
            text = "✓ Не нужно вводить email или телефон",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
        TextButton(onClick = onPasteLink, enabled = !isLoading, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("У меня есть ссылка", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ServerSelectorCard(
    serverName: String,
    isAutoMode: Boolean,
    ping: String?,
    exitIp: String?,
    exitCountry: String?,
    isRunning: Boolean,
    onClick: () -> Unit,
) {
    val isEmergency = !isAutoMode && isEmergencyServerName(serverName)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = if (isEmergency) colorPingRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.background,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isEmergency) colorPingRed.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.surfaceContainer
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isAutoMode) "⚡" else flagForServerName(serverName),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEmergency) "Аварийный режим" else "Сервер",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isEmergency) colorPingRed else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (isAutoMode) "Авто · самый быстрый" else serverDisplayName(serverName),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isEmergency) colorPingRed else Color.Unspecified,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // бывшая карточка «Ваш IP для сайтов»: адрес выхода, страна — если её нет в названии сервера
                if (isRunning) {
                    val (flag, country) = countryLabel(exitCountry)
                    val shown = if (isAutoMode) serverName else serverDisplayName(serverName)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape)
                            .background(if (exitIp != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline))
                        Text(
                            text = when {
                                exitIp == null -> "IP для сайтов: определяем…"
                                country.isNotEmpty() && !shown.contains(country) -> "IP для сайтов: $exitIp · $flag $country"
                                else -> "IP для сайтов: $exitIp"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (ping != null && !isAutoMode) {
                Text(text = ping, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * NEXALINK: окно новости с сайта — в стиле приложения: значок по типу, заголовок,
 * текст, кнопка-«таблетка» и «Поделиться с друзьями» (ссылка на nexalink.online).
 */
@Composable
private fun AnnouncementDialog(type: String?, title: String, body: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val (emoji, tint) = when (type) {
        "warning" -> "!" to MaterialTheme.colorScheme.secondary
        "success" -> "✓" to MaterialTheme.colorScheme.tertiary
        "promo" -> "★" to MaterialTheme.colorScheme.secondary
        else -> "i" to MaterialTheme.colorScheme.primary
    }
    // эмодзи в конце заголовка с сайта оставляем, а значок в начале рисуем сами
    val cleanTitle = title.trim()
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.background, tonalElevation = 0.dp,
            shadowElevation = 12.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.nexalink_logo), contentDescription = null,
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)),
                    )
                    Text("NEXALINK", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center) {
                        Text(emoji, color = tint, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(cleanTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(22.dp))
                Button(onClick = onDismiss, shape = RoundedCornerShape(99.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text("Понятно", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                TextButton(
                    onClick = {
                        val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(android.content.Intent.EXTRA_TEXT, "NEXALINK — бесплатный VPN без регистрации: https://nexalink.online")
                        context.startActivity(android.content.Intent.createChooser(send, "Поделиться NEXALINK"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Поделиться с друзьями", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}


/** «Поделиться NEXALINK» — мягкая кнопка-таблетка под карточкой сервера, открывает шторку приглашения. */
@Composable
private fun ShareButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(99.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(99.dp),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
    ) {
        Row(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
            Icon(painter = painterResource(R.drawable.ic_share_24dp), contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text("Поделиться NEXALINK с друзьями", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary)
        }
    }
}
