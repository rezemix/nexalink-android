package online.nexalink.app.handler

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import online.nexalink.app.AppConfig
import online.nexalink.app.util.LogUtil

/**
 * NEXALINK: раньше кнопка "Обновить" просто открывала ссылку в браузере —
 * пользователь качал APK сам и потом сам же его ставил, и было непонятно,
 * работает ли это вообще (долгая загрузка выглядела как зависание). Теперь
 * качаем прямо в приложении через системный DownloadManager и опрашиваем
 * его на предмет прогресса — на экране видно "Скачивание… 42%", затем
 * "Установка…", а не тишина.
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

        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("Обновление NEXALINK")
            setDescription("Скачивание версии $versionName")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(appContext, android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
            setMimeType("application/vnd.android.package-archive")
            setAllowedOverMetered(true)
        }

        val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = try {
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to enqueue update download", e)
            _downloadState.value = UpdateDownloadState.Failed("Не удалось начать скачивание")
            return
        }

        _downloadState.value = UpdateDownloadState.Downloading(0)

        scope.launch {
            pollUntilDone(appContext, downloadManager, downloadId)
        }
    }

    private suspend fun pollUntilDone(context: Context, downloadManager: DownloadManager, downloadId: Long) {
        while (true) {
            val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
            if (cursor == null) {
                _downloadState.value = UpdateDownloadState.Failed("Не удалось скачать обновление")
                return
            }
            if (!cursor.moveToFirst()) {
                cursor.close()
                _downloadState.value = UpdateDownloadState.Failed("Не удалось скачать обновление")
                return
            }

            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val bytesSoFar = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            cursor.close()

            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    _downloadState.value = UpdateDownloadState.Installing
                    val uri = try {
                        downloadManager.getUriForDownloadedFile(downloadId)
                    } catch (e: Exception) {
                        LogUtil.e(AppConfig.TAG, "getUriForDownloadedFile failed", e)
                        null
                    }
                    if (uri == null) {
                        _downloadState.value = UpdateDownloadState.Failed("Файл скачался, но не открылся")
                        return
                    }
                    promptInstall(context, uri)
                    _downloadState.value = UpdateDownloadState.Idle
                    return
                }
                DownloadManager.STATUS_FAILED -> {
                    _downloadState.value = UpdateDownloadState.Failed("Скачивание не удалось, попробуйте ещё раз")
                    return
                }
                else -> {
                    val progress = if (totalBytes > 0) ((bytesSoFar * 100) / totalBytes).toInt() else 0
                    _downloadState.value = UpdateDownloadState.Downloading(progress)
                }
            }
            delay(350L)
        }
    }

    private fun promptInstall(context: Context, apkUri: Uri) {
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
