package online.nexalink.app.receiver

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import online.nexalink.app.core.CoreServiceManager
import online.nexalink.app.handler.TrafficStatsHub

/** NEXALINK: виджет 4×1. Кнопка шлёт тот же клик, что и 1×1 (WidgetProvider), обновляет оба WidgetUpdater. */
class WideWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetUpdater.update(context, CoreServiceManager.isRunning(), TrafficStatsHub.serverName)
    }
}
