package online.nexalink.app.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import online.nexalink.app.AppConfig
import online.nexalink.app.R

/**
 * NEXALINK: оба виджета рабочего стола — 1×1 (кнопка) и 4×1 (статус, сервер, скорость).
 * Работает в процессе VPN: туда приходят и смена состояния, и тики статистики.
 */
object WidgetUpdater {

    fun update(context: Context, running: Boolean, serverName: String? = null, speed: String? = null) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val click = PendingIntent.getBroadcast(
            context, R.id.layout_switch,
            Intent(context, WidgetProvider::class.java).setAction(AppConfig.BROADCAST_ACTION_WIDGET_CLICK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val btn = if (running) R.drawable.widget_btn_on else R.drawable.widget_btn_off

        val smallIds = manager.getAppWidgetIds(ComponentName(context, WidgetProvider::class.java))
        if (smallIds.isNotEmpty()) {
            val small = RemoteViews(context.packageName, R.layout.widget_switch)
            small.setOnClickPendingIntent(R.id.layout_switch, click)
            small.setInt(R.id.image_switch, "setBackgroundResource", btn)
            small.setTextColor(R.id.text_label, if (running) 0xFF1D9A54.toInt() else 0xFF0A1F3A.toInt())
            smallIds.forEach { manager.updateAppWidget(it, small) }
        }

        val wideIds = manager.getAppWidgetIds(ComponentName(context, WideWidgetProvider::class.java))
        if (wideIds.isNotEmpty()) {
            val wide = RemoteViews(context.packageName, R.layout.widget_wide)
            wide.setOnClickPendingIntent(R.id.wide_button, click)
            wide.setOnClickPendingIntent(R.id.wide_root, PendingIntent.getActivity(
                context, R.id.wide_root,
                context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent(),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ))
            wide.setInt(R.id.wide_button, "setBackgroundResource", btn)
            wide.setTextViewText(R.id.wide_title,
                if (running) listOfNotNull("Подключено", serverName).joinToString(" · ") else "Не подключено")
            wide.setTextViewText(R.id.wide_subtitle,
                if (running) "NEXALINK" + (speed?.let { " · ↓ $it" } ?: "") else "NEXALINK · нажмите, чтобы включить")
            wide.setTextColor(R.id.wide_subtitle, if (running && speed != null) 0xFF1D9A54.toInt() else 0xFF566074.toInt())
            wideIds.forEach { manager.updateAppWidget(it, wide) }
        }
    }
}
