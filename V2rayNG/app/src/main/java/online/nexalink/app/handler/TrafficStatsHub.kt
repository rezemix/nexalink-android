package online.nexalink.app.handler

import android.content.Context
import online.nexalink.app.AppConfig
import online.nexalink.app.helper.MessageHelper
import online.nexalink.app.receiver.WidgetUpdater
import online.nexalink.app.ui.main.serverDisplayName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * NEXALINK: живая статистика подключения в процессе VPN — скорость, трафик за подключение
 * и за сегодня. Раз в QUERY_INTERVAL_MS её получает главный экран (MSG_STATS) и виджеты.
 * «Сегодня» хранится в своих SharedPreferences этого процесса (MMKV — у UI-процесса).
 */
object TrafficStatsHub {

    private const val PREFS = "nexalink_traffic"
    private const val KEY_DAY = "day"
    private const val KEY_BYTES = "bytes"

    @Volatile var serverName: String? = null
        private set
    private var sessionBytes = 0L
    private var sessionStartMs = 0L

    /** Новое подключение: обнуляем счётчик сессии, запоминаем сервер для виджета. */
    fun onConnected(context: Context, remarks: String?) {
        serverName = remarks?.let { serverDisplayName(it) }
        sessionBytes = 0L
        sessionStartMs = System.currentTimeMillis()
        WidgetUpdater.update(context, running = true, serverName = serverName, speed = null)
    }

    fun onTick(context: Context, upBps: Double, downBps: Double, deltaBytes: Long) {
        sessionBytes += deltaBytes
        val today = addToday(context, deltaBytes)
        MessageHelper.sendMsg2UI(context, AppConfig.MSG_STATS,
            "${downBps.toLong()}|${upBps.toLong()}|$sessionBytes|$today|$sessionStartMs")
        WidgetUpdater.update(context, running = true, serverName = serverName, speed = formatSpeed(downBps))
    }

    private fun addToday(context: Context, delta: Long): Long {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val total = (if (prefs.getString(KEY_DAY, null) == day) prefs.getLong(KEY_BYTES, 0L) else 0L) + delta
        prefs.edit().putString(KEY_DAY, day).putLong(KEY_BYTES, total).apply()
        return total
    }

    fun sessionText(): String = when {
        sessionBytes >= 1L shl 30 -> String.format(Locale("ru"), "%.1f ГБ", sessionBytes / (1L shl 30).toDouble())
        sessionBytes >= 1L shl 20 -> String.format(Locale("ru"), "%.0f МБ", sessionBytes / (1L shl 20).toDouble())
        else -> String.format(Locale("ru"), "%.0f КБ", sessionBytes / 1024.0)
    }

    /** Байт/с → «24,6 Мбит/с». */
    fun formatSpeed(bytesPerSec: Double): String {
        val bits = bytesPerSec * 8
        return when {
            bits >= 1_000_000 -> String.format(Locale("ru"), "%.1f Мбит/с", bits / 1_000_000)
            bits >= 1_000 -> String.format(Locale("ru"), "%.0f Кбит/с", bits / 1_000)
            else -> "0 Кбит/с"
        }
    }
}
