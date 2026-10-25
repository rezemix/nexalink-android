package online.nexalink.app.handler

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import online.nexalink.app.AppConfig
import online.nexalink.app.util.LogUtil
import online.nexalink.app.util.Utils
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * NEXALINK: расширенный список рекламы и трекеров (AdGuard DNS filter + RU AdList),
 * тот же, что на серверах. Панель собирает его раз в сутки; приложение скачивает
 * его раз в сутки и подключает к правилу «Блокировать рекламу» (CoreConfigManager).
 *
 * Файл принимается, только если sha256 совпал с заголовком X-Content-SHA256 и формат
 * похож на geosite — испорченный файл не даст ядру запуститься.
 */
object AdListUpdater {

    private const val KEY_LAST_CHECK = "nexalink_ads_last_check"
    private const val CHECK_INTERVAL_MS = 24 * 3600 * 1000L
    private const val MIN_SIZE = 1024L

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private fun file(context: Context) = File(Utils.userAssetPath(context), AppConfig.NEXALINK_ADS_DAT)

    /** Файл скачан и выглядит целым — можно ссылаться на него из правил. */
    fun isReady(context: Context): Boolean {
        val f = file(context)
        return f.exists() && f.length() >= MIN_SIZE
    }

    /** Раз в сутки проверяет и при необходимости обновляет список. Вызывать в фоне. */
    fun updateIfNeeded(context: Context) {
        val now = System.currentTimeMillis()
        val last = MmkvManager.decodeSettingsLong(KEY_LAST_CHECK, 0L)
        if (isReady(context) && now - last < CHECK_INTERVAL_MS) return
        try {
            val request = Request.Builder().url(AppConfig.NEXALINK_ADS_URL).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    LogUtil.w(AppConfig.TAG, "AdListUpdater: HTTP ${response.code}")
                    return
                }
                val expected = response.header("X-Content-SHA256").orEmpty().lowercase()
                val bytes = response.body?.bytes() ?: return
                val actual = MessageDigest.getInstance("SHA-256").digest(bytes)
                    .joinToString("") { "%02x".format(it) }
                // GeoSiteList: первое поле — entry (тег 0x0A)
                if (expected.isEmpty() || actual != expected || bytes.size < MIN_SIZE || bytes[0] != 0x0A.toByte()) {
                    LogUtil.w(AppConfig.TAG, "AdListUpdater: файл отклонён (sha/формат)")
                    return
                }
                val target = file(context)
                val tmp = File(target.parentFile, target.name + ".tmp")
                tmp.writeBytes(bytes)
                if (!tmp.renameTo(target)) {
                    target.writeBytes(bytes)
                    tmp.delete()
                }
                MmkvManager.encodeSettings(KEY_LAST_CHECK, now)
                LogUtil.i(AppConfig.TAG, "AdListUpdater: список обновлён (${bytes.size} байт)")
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "AdListUpdater: не удалось обновить", e)
        }
    }
}
