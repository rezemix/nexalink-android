package online.nexalink.app.util

import android.os.Build
import online.nexalink.app.handler.MmkvManager
import java.util.UUID

/**
 * Стабильный отпечаток устройства для лимита количества устройств RemnaWave
 * (Settings → HWID Device Limit на панели). Панель принимает эти заголовки
 * при запросе подписки и считает уникальные x-hwid как отдельные устройства.
 *
 * Подробности: https://docs.remna.st/panel/features/hwid-device-limit
 */
object DeviceUtil {

    private const val KEY_HWID = "device_hwid"

    /**
     * Возвращает стабильный UUID для этой установки приложения — генерируется
     * один раз и хранится постоянно, не привязан к железу физически (переустановка
     * даст новый hwid), но этого достаточно для контроля количества активных
     * устройств на подписке.
     */
    fun getOrCreateHwid(): String {
        val existing = MmkvManager.decodeSettingsString(KEY_HWID)
        if (!existing.isNullOrBlank()) {
            return existing
        }
        val generated = UUID.randomUUID().toString()
        MmkvManager.encodeSettings(KEY_HWID, generated)
        return generated
    }

    /**
     * Заголовки, которые RemnaWave ожидает при запросе подписки для учёта
     * устройства в лимите (x-hwid обязателен, остальные — для отображения
     * в панели администратору).
     */
    fun hwidHeaders(): Map<String, String> {
        return mapOf(
            "x-hwid" to getOrCreateHwid(),
            "x-device-os" to "Android",
            "x-ver-os" to Build.VERSION.RELEASE.orEmpty(),
            "x-device-model" to "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
        )
    }
}
