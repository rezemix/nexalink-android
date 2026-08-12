package online.nexalink.app.handler

import online.nexalink.app.AppConfig
import online.nexalink.app.BuildConfig
import online.nexalink.app.dto.CheckUpdateResult
import online.nexalink.app.dto.NexalinkVersionInfo
import online.nexalink.app.dto.UrlContentRequest
import online.nexalink.app.util.HttpUtil
import online.nexalink.app.util.JsonUtil
import online.nexalink.app.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UpdateCheckerManager {
    // NEXALINK: параметр includePreRelease оставлен только чтобы не трогать
    // вызывающий код (CheckUpdateViewModel/автопроверка) — у нашей простой
    // точки проверки нет отдельного пре-релиз канала, игнорируется.
    suspend fun checkForUpdate(includePreRelease: Boolean = false): CheckUpdateResult = withContext(Dispatchers.IO) {
        val url = AppConfig.NEXALINK_UPDATE_CHECK_URL

        var response = HttpUtil.getUrlContent(
            UrlContentRequest(url = url, timeout = 5000)
        )
        if (response.isNullOrEmpty()) {
            // На случай, если прямой доступ к сайту сейчас недоступен (например,
            // во время БПЛА-ограничений) — пробуем через прокси активного
            // соединения, если оно есть.
            val proxyUsername = SettingsManager.getSocksUsername()
            val proxyPassword = SettingsManager.getSocksPassword()
            val httpPort = SettingsManager.getHttpPort()
            response = HttpUtil.getUrlContent(
                UrlContentRequest(
                    url = url,
                    timeout = 5000,
                    httpPort = httpPort,
                    proxyUsername = proxyUsername,
                    proxyPassword = proxyPassword
                )
            )
        }
        if (response.isNullOrEmpty()) {
            return@withContext CheckUpdateResult(hasUpdate = false)
        }

        val info = JsonUtil.fromJsonSafe(response, NexalinkVersionInfo::class.java)
            ?: return@withContext CheckUpdateResult(hasUpdate = false)

        LogUtil.i(
            AppConfig.TAG,
            "Found version: ${info.version} (current: ${BuildConfig.VERSION_NAME})"
        )

        return@withContext if (compareVersions(info.version, BuildConfig.VERSION_NAME) > 0) {
            CheckUpdateResult(
                hasUpdate = true,
                latestVersion = info.version,
                releaseNotes = info.notes,
                downloadUrl = info.downloadUrl
            )
        } else {
            CheckUpdateResult(hasUpdate = false)
        }
    }

    private fun compareVersions(version1: String, version2: String): Int {
        val v1 = version1.split(".")
        val v2 = version2.split(".")

        for (i in 0 until maxOf(v1.size, v2.size)) {
            val num1 = (i < v1.size).let { if (it) v1[i].toIntOrNull() ?: 0 else 0 }
            val num2 = (i < v2.size).let { if (it) v2[i].toIntOrNull() ?: 0 else 0 }
            if (num1 != num2) return num1 - num2
        }
        return 0
    }
}
