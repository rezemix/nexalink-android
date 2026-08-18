package online.nexalink.app.handler

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import online.nexalink.app.AppConfig
import online.nexalink.app.dto.UrlContentRequest
import online.nexalink.app.util.HttpUtil
import online.nexalink.app.util.LogUtil
import java.io.File

/**
 * NEXALINK: раньше кнопка "Обновить" просто открывала ссылку в браузере —
 * пользователь качал APK сам и потом сам же его ставил, и было непонятно,
 * работает ли это вообще (долгая загрузка выглядела как зависание). Потом
 * переключили на системный DownloadManager с опросом прогресса.
 *
 * 18.08.2026: обнаружено, что системный DownloadManager зависает на 0%,
 * пока активен VPN-туннель самого приложения (подтверждено: обычный
 * прокси-трафик приложения при этом работает нормально — DownloadManager
 * как отдельный системный сервис не всегда корректно ходит поверх
 * VpnService-туннеля). Качаем теперь сами, тем же локальным HTTP/SOCKS-
 * прокси, что использует весь остальной трафик приложения (тот же
 * механизм, что уже проверен на подписке/geo-файлах в HttpUtil) — надёжнее,
 * чем полагаться на системный сервис в обход собственного туннеля.
 */
sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState
    data class Downloading(val progressPercent: Int) : UpdateDownloadState
    data object Installing : UpdateDownloadState
    data class Failed(val message: String) : UpdateDownloadState
}

object ApkUpdateInstaller {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    fun dismiss() {
        _downloadState.value = UpdateDownloadState.Idle
    }

    fun downloadAndInstall(context: Context, downloadUrl: String, versionName: String) {
        if (_downloadState.value is UpdateDownloadState.Downloading ||
            _downloadState.value is UpdateDownloadState.Installing
        ) {
            return // уже качаем/ставим — второй раз не запускаем
        }

        val appContext = context.applicationContext
        val fileName = "NEXALINK_$versionName.apk"
        // Кэш-директория — под неё уже настроен FileProvider (cache_paths.xml),
        // ничего дополнительно в манифесте объявлять не нужно.
        val targetFile = File(appContext.cacheDir, fileName)

        _downloadState.value = UpdateDownloadState.Downloading(0)

        scope.launch {
            val httpPort = SettingsManager.getHttpPort()
            val proxyUsername = SettingsManager.getSocksUsername()
            val proxyPassword = SettingsManager.getSocksPassword()

            var lastReported = -1
            val success = try {
                HttpUtil.downloadFileWithProgress(
                    UrlContentRequest(
                        url = downloadUrl,
                        timeout = 30000,
                        httpPort = httpPort,
                        proxyUsername = proxyUsername,
                        proxyPassword = proxyPassword,
                    ),
                    targetFile,
                ) { bytesSoFar, totalBytes ->
                    val pct = if (totalBytes > 0) ((bytesSoFar * 100) / totalBytes).toInt() else 0
                    if (pct != lastReported) {
                        lastReported = pct
                        _downloadState.value = UpdateDownloadState.Downloading(pct)
                    }
                }
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Update download failed", e)
                false
            }

            if (!success) {
                targetFile.delete()
                _downloadState.value = UpdateDownloadState.Failed("Скачивание не удалось, попробуйте ещё раз")
                return@launch
            }

            _downloadState.value = UpdateDownloadState.Installing
            val uri = try {
                FileProvider.getUriForFile(appContext, "${appContext.packageName}.cache", targetFile)
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "FileProvider getUriForFile failed", e)
                null
            }
            if (uri == null) {
                _downloadState.value = UpdateDownloadState.Failed("Файл скачался, но не открылся")
                return@launch
            }
            promptInstall(appContext, uri)
            _downloadState.value = UpdateDownloadState.Idle
        }
    }

    private fun promptInstall(context: Context, apkUri: android.net.Uri) {
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(installIntent)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to launch package installer", e)
            Toast.makeText(context, "Обновление скачано, но не удалось запустить установку", Toast.LENGTH_LONG).show()
        }
    }
}
