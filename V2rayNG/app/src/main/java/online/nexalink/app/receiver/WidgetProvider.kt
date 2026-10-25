package online.nexalink.app.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import online.nexalink.app.AppConfig
import online.nexalink.app.R
import online.nexalink.app.core.CoreServiceManager
import online.nexalink.app.core.LauncherManager

class WidgetProvider : AppWidgetProvider() {
    /**
     * This method is called every time the widget is updated.
     * It updates the widget background based on the V2Ray service running state.
     *
     * @param context The Context in which the receiver is running.
     * @param appWidgetManager The AppWidgetManager instance.
     * @param appWidgetIds The appWidgetIds for which an update is needed.
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetUpdater.update(context, CoreServiceManager.isRunning(), online.nexalink.app.handler.TrafficStatsHub.serverName)
    }


    /**
     * This method is called when the BroadcastReceiver is receiving an Intent broadcast.
     * It handles widget click actions and updates the widget background based on the V2Ray service state.
     *
     * @param context The Context in which the receiver is running.
     * @param intent The Intent being received.
     */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (AppConfig.BROADCAST_ACTION_WIDGET_CLICK == intent.action) {
            if (CoreServiceManager.isRunning()) {
                LauncherManager.stopService(context)
            } else {
                LauncherManager.startServiceFromToggle(context)
            }
        } else if (AppConfig.BROADCAST_ACTION_ACTIVITY == intent.action) {
            AppWidgetManager.getInstance(context)?.let { manager ->
                when (intent.getIntExtra("key", 0)) {
                    AppConfig.MSG_STATE_RUNNING, AppConfig.MSG_STATE_START_SUCCESS -> {
                        WidgetUpdater.update(context, true, online.nexalink.app.handler.TrafficStatsHub.serverName)
                    }

                    AppConfig.MSG_STATE_NOT_RUNNING, AppConfig.MSG_STATE_START_FAILURE, AppConfig.MSG_STATE_STOP_SUCCESS -> {
                        WidgetUpdater.update(context, false)
                    }
                }
            }
        }
    }
}
