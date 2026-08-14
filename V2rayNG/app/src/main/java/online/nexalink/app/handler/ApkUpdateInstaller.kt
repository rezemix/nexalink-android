package online.nexalink.app.handler

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.ContextCompat
import online.nexalink.app.AppConfig
import online.nexalink.app.util.LogUtil

/**
 * NEXALINK: раньше кнопка "Обновить" просто открывала ссылку в браузере —
 * пользователь качал APK сам и потом сам же его ставил. Теперь скачиваем
 * прямо в приложении через системный DownloadManager (фоновая скачка,
 * системный прогресс в шторке уведомлений) и сразу предлагаем установку,
 * как только файл готов.
 */
object ApkUpdateInstaller {
    private var receiver: BroadcastReceiver? = null

    fun downloadAndInstall(context: Context, downloadUrl: String, versionName: String) {
        val fileName = "NEXALINK_$versionName.apk"

        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("Обновление NEXALINK")
            setDescription("Скачивание версии $versionName")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setMimeType("application/vnd.android.package-archive")
            setAllowedOverMetered(true)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = try {
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to enqueue update download", e)
            Toast.makeText(context, "Не удалось начать скачивание обновления", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(context, "Скачивание обновления начато…", Toast.LENGTH_SHORT).show()

        val appContext = context.applicationContext
        val newReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val finishedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (finishedId != downloadId) return
                try {
                    appContext.unregisterReceiver(this)
                } catch (_: Exception) {
                }
                receiver = null

                val uri = try {
                    downloadManager.getUriForDownloadedFile(downloadId)
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "getUriForDownloadedFile failed", e)
                    null
                }
                if (uri == null) {
                    Toast.makeText(appContext, "Не удалось скачать обновление, попробуйте ещё раз", Toast.LENGTH_LONG).show()
                    return
                }
                promptInstall(appContext, uri)
            }
        }
        receiver = newReceiver
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(appContext, newReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            appContext.registerReceiver(newReceiver, filter)
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
