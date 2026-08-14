package online.nexalink.app.handler

import android.content.Context
import online.nexalink.app.AppConfig
import online.nexalink.app.dto.UrlContentRequest
import online.nexalink.app.dto.entities.AssetUrlItem
import online.nexalink.app.extension.concatUrl
import online.nexalink.app.util.HttpUtil
import online.nexalink.app.util.LogUtil
import online.nexalink.app.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * NEXALINK: тихое фоновое обновление geosite.dat/geoip.dat — раньше пользователю
 * приходилось заходить в Настройки → Файлы-ассеты и жать "скачать" вручную,
 * иначе списки российских/зарубежных доменов постепенно устаревают и каскады
 * (Russia/Russia 2/Pi4-relay) начинают неточно определять, что слать DIRECT,
 * а что — через каскад в EU. Логика скачивания повторяет UserAssetViewModel,
 * но без привязки к открытому экрану — вызывается сама при удачном подключении.
 */
object GeoFilesUpdater {

    private val UPDATE_INTERVAL_MS = 7L * 24 * 60 * 60 * 1000 // раз в неделю достаточно

    private val builtInGeoFiles = listOf(AppConfig.GEOSITE_DAT, AppConfig.GEOIP_DAT, AppConfig.GEOIP_ONLY_CN_PRIVATE_DAT)

    /**
     * Проверяет, не пора ли обновиться, и если да — тихо качает новые geo-файлы
     * через уже поднятый локальный прокси (httpPort). Без тостов и диалогов —
     * пользователь не должен ничего замечать, если всё прошло гладко.
     */
    suspend fun maybeUpdate(context: Context) = withContext(Dispatchers.IO) {
        val lastUpdate = MmkvManager.decodeSettingsLong(AppConfig.PREF_LAST_GEO_UPDATE_EPOCH, 0L)
        if (System.currentTimeMillis() - lastUpdate < UPDATE_INTERVAL_MS) return@withContext

        try {
            val geoFilesSource = MmkvManager.decodeSettingsString(AppConfig.PREF_GEO_FILES_SOURCES)
                ?: AppConfig.GEO_FILES_SOURCES.first()
            val extDir = File(Utils.userAssetPath(context))
            val savedAssets = MmkvManager.decodeAssetUrls()

            val items = builtInGeoFiles
                .filter { geoFile -> savedAssets.none { it.assetUrl.remarks == geoFile } }
                .map {
                    AssetUrlItem(
                        it,
                        String.format(AppConfig.GITHUB_DOWNLOAD_URL, geoFilesSource).concatUrl(it),
                        locked = true
                    )
                }

            if (items.isEmpty()) return@withContext

            val httpPort = SettingsManager.getHttpPort()
            val proxyUsername = SettingsManager.getSocksUsername()
            val proxyPassword = SettingsManager.getSocksPassword()

            var successCount = 0
            items.forEach { item ->
                val portsToTry = if (httpPort == 0) listOf(0) else listOf(httpPort, 0)
                val ok = portsToTry.any { port -> tryDownload(item, extDir, port, proxyUsername, proxyPassword) }
                if (ok) successCount++
            }

            // Отмечаем попытку в любом случае — чтобы при системном сбое (нет
            // сети сейчас) не долбить GitHub каждым новым подключением, а не
            // раньше следующей недели. Частичный успех тоже считается: то, что
            // не скачалось сейчас, подтянется в следующий раз.
            MmkvManager.encodeSettings(AppConfig.PREF_LAST_GEO_UPDATE_EPOCH, System.currentTimeMillis())
            LogUtil.i(AppConfig.TAG, "GeoFilesUpdater: обновлено $successCount из ${items.size} файлов")
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "GeoFilesUpdater: сбой тихого обновления", e)
        }
    }

    private fun tryDownload(
        item: AssetUrlItem,
        extDir: File,
        httpPort: Int,
        proxyUsername: String?,
        proxyPassword: String?
    ): Boolean {
        val targetTemp = File(extDir, item.remarks + "_temp")
        val target = File(extDir, item.remarks)
        return try {
            val done = HttpUtil.downloadToFile(
                UrlContentRequest(
                    url = item.url,
                    timeout = 15000,
                    httpPort = httpPort,
                    proxyUsername = proxyUsername,
                    proxyPassword = proxyPassword
                ),
                targetTemp
            )
            if (done) targetTemp.renameTo(target) else targetTemp.delete()
            done
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "GeoFilesUpdater: не скачался ${item.remarks}", e)
            false
        }
    }
}
