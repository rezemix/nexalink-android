package online.nexalink.app.ui.main

import online.nexalink.app.dto.AnnouncementInfo
import online.nexalink.app.dto.CheckUpdateResult
import online.nexalink.app.dto.GroupMapItem
import online.nexalink.app.dto.LocateTarget

/**
 * Main UI state
 */
data class MainUiState(
    val groups: List<GroupMapItem> = emptyList(),
    val selectedGroupId: String = "",
    val selectedGuid: String? = null,
    val isRunning: Boolean = false,
    val isTesting: Boolean = false,
    val statusText: String = "",
    val locateTarget: LocateTarget? = null,
    val confirmRemove: Boolean = false,
    val doubleColumnDisplay: Boolean = false,
    val shareQRCodeBitmap: android.graphics.Bitmap? = null,
    // NEXALINK: авто-режим сам подбирает самый быстрый сервер по последнему
    // пингу; выключается автоматически, если пользователь выбрал сервер вручную.
    val isAutoMode: Boolean = true,
    // NEXALINK: результат автопроверки обновления при запуске — если есть
    // новая версия, на главном экране всплывает диалог с кнопкой "Обновить".
    val availableUpdate: CheckUpdateResult? = null,
    // NEXALINK: активное объявление с сайта (например, "тестируем на боевых нодах"),
    // ещё не показанное этому пользователю — всплывает один раз на id.
    val activeAnnouncement: AnnouncementInfo? = null
)

/**
 * All possible user interaction intents
 */
sealed interface MainAction {
    data object Initialize : MainAction
    data object RefreshGroups : MainAction
    data object ToggleService : MainAction
    data object TestCurrentServer : MainAction
    data object TestAllServers : MainAction
    data object TestRealAllServers : MainAction
    data object CancelTesting : MainAction
    data object RemoveAllServers : MainAction
    data object RemoveDuplicateServers : MainAction
    data object RemoveInvalidServers : MainAction
    data object SortByTestResults : MainAction
    data object UpdateSubscriptions : MainAction
    data object ExportAll : MainAction

    data object ImportQRcode : MainAction
    data object ImportClipboard : MainAction
    data object ImportConfigLocal : MainAction
    data class ImportManually(val type: Int) : MainAction
    data object RestartService : MainAction
    data object LocateSelectedServer : MainAction

    data class SelectGroup(val groupId: String) : MainAction
    data class SelectServer(val guid: String) : MainAction
    data class SetAutoMode(val enabled: Boolean) : MainAction
    data class RemoveServer(val guid: String) : MainAction
    data class EditServer(val guid: String, val profile: online.nexalink.app.dto.entities.ProfileItem) : MainAction
    data class Search(val query: String) : MainAction
    data class ShareQRCode(val guid: String) : MainAction
    data class ShareClipboard(val guid: String) : MainAction
    data class ShareFullContent(val guid: String) : MainAction
    data object DismissQRCodeDialog : MainAction
    data object DismissUpdateDialog : MainAction
    data object DismissAnnouncementDialog : MainAction

    data class ImportBatchConfig(val configText: String) : MainAction

    data class LocateHandled(val target: LocateTarget) : MainAction
}
