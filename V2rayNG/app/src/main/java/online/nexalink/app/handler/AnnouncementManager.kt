package online.nexalink.app.handler

import online.nexalink.app.AppConfig
import online.nexalink.app.dto.AnnouncementInfo
import online.nexalink.app.dto.UrlContentRequest
import online.nexalink.app.util.HttpUtil
import online.nexalink.app.util.JsonUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * NEXALINK: показ объявлений с сайта в приложении (например, "мы тестируем на
 * боевых нодах" — редкие уведомления, не чаще чем реально нужно). Один и тот
 * же id объявления показывается пользователю только один раз.
 */
object AnnouncementManager {
    suspend fun checkForAnnouncement(): AnnouncementInfo? = withContext(Dispatchers.IO) {
        val url = AppConfig.NEXALINK_ANNOUNCEMENT_URL

        var response = HttpUtil.getUrlContent(
            UrlContentRequest(url = url, timeout = 5000)
        )
        if (response.isNullOrEmpty()) {
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
        if (response.isNullOrEmpty() || response == "null") {
            return@withContext null
        }

        JsonUtil.fromJsonSafe(response, AnnouncementInfo::class.java)
    }
}
