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

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showServerPicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MainDrawerContent(
                drawerState = drawerState,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    onNavigate(route)
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                HomeTopBar(
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onAccountClick = { onNavigate(MainDestination.NexalinkAccount) },
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val daysLeft = uiState.subscriptionDaysLeft
                if (daysLeft != null) {
                    SubscriptionExpiryBanner(
                        daysLeft = daysLeft,
                        onDismiss = { onAction(MainAction.DismissSubscriptionBanner) },
                    )
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ConnectButton(
                            isRunning = isRunning,
                            onClick = { onAction(MainAction.ToggleService) },
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        Text(
                            text = if (isRunning) "Подключено" else "Отключено",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isRunning) colorConnected else MaterialTheme.colorScheme.onSurface,
                        )
                        if (statusText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                ServerSelectorCard(
                    serverName = currentServerName,
                    isAutoMode = uiState.isAutoMode,
                    onClick = { showServerPicker = true },
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

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
        val icon = when (activeAnnouncement.type) {
            "warning" -> "⚠️"
            "success" -> "✅"
            "promo" -> "🎁"
            else -> "📢"
        }
        AlertDialog(
            onDismissRequest = { onAction(MainAction.DismissAnnouncementDialog) },
            title = { Text("$icon ${activeAnnouncement.title}") },
            text = { Text(activeAnnouncement.body) },
            confirmButton = {
                TextButton(onClick = { onAction(MainAction.DismissAnnouncementDialog) }) {
                    Text("Понятно")
                }
            },
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
            Icon(painter = painterResource(R.drawable.ic_menu_24dp), contentDescription = null)
        }
        Text(
            text = "NEXALINK",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        IconButton(onClick = onAccountClick) {
            Icon(painter = painterResource(R.drawable.ic_lock_24dp), contentDescription = null)
        }
    }
}

@Composable
private fun ConnectButton(
    isRunning: Boolean,
    onClick: () -> Unit,
) {
    val targetColor = if (isRunning) colorConnected else colorConnecting
    val buttonColor by animateColorAsState(targetValue = targetColor, label = "connectButtonColor")

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    Box(contentAlignment = Alignment.Center) {
        if (isRunning) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(buttonColor.copy(alpha = 0.16f))
            )
        }
        Surface(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            color = buttonColor,
            shadowElevation = 12.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(if (isRunning) R.drawable.ic_stop_24dp else R.drawable.ic_play_24dp),
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(64.dp),
                )
            }
        }
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

@Composable
private fun ServerSelectorCard(
    serverName: String,
    isAutoMode: Boolean,
    onClick: () -> Unit,
) {
    val isEmergency = !isAutoMode && isEmergencyServerName(serverName)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = if (isEmergency) colorPingRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isEmergency) colorPingRed.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.primaryContainer
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
                    text = if (isAutoMode) "Авто" else if (isEmergency) "Аварийный режим" else "Сервер",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isEmergency) colorPingRed else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = serverName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isEmergency) colorPingRed else Color.Unspecified,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
