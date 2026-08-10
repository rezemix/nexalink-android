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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onSelect: (String) -> Unit,
    onTestAll: () -> Unit,
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(servers, key = { it.guid }) { server ->
                        ServerPickerRow(
                            server = server,
                            isSelected = server.guid == selectedGuid,
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

@Composable
private fun ServerPickerRow(
    server: ServersCache,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                else Color.Transparent
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = flagForServerName(server.profile.remarks), style = MaterialTheme.typography.titleMedium)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = server.profile.remarks,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (server.testDelayString.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(
                        (if (server.testDelayMillis < 0L) colorPingRed else colorPing).copy(alpha = 0.12f)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = server.testDelayString,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (server.testDelayMillis < 0L) colorPingRed else colorPing,
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Грубое сопоставление названия сервера со страной по ключевым словам в remarks. */
fun flagForServerName(remarks: String): String {
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
