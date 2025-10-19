package com.prometeo.batterywidget

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log

/**
 * Battery Monitoring Service
 *
 * Background service that listens for battery change events and updates
 * widgets in AUTO mode in real-time. This ensures immediate updates when
 * battery status changes without relying on periodic polling.
 */
class BatteryMonitorService : Service() {

    private var lastUpdateTime = 0L
    private val MIN_UPDATE_INTERVAL = 1000L

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val currentTime = System.currentTimeMillis()

                // Anti-rebond
                if (currentTime - lastUpdateTime < MIN_UPDATE_INTERVAL) {
                    return
                }
                lastUpdateTime = currentTime

                when (intent.action) {
                    Intent.ACTION_BATTERY_CHANGED,
                    Intent.ACTION_BATTERY_LOW,
                    Intent.ACTION_BATTERY_OKAY,
                    Intent.ACTION_POWER_CONNECTED,
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        Log.d("BatteryMonitorService", "Battery change: ${intent.action}")
                        updateWidgetsInAutoMode(context)
                    }
                }
            } catch (e: Exception) {
                Log.e("BatteryMonitorService", "Error in battery receiver", e)
            }
        }
    }

    private fun updateWidgetsInAutoMode(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, BatteryWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

        // Créer une seule instance du provider
        val widgetProvider = BatteryWidgetProvider()

        appWidgetIds.forEach { appWidgetId ->
            val prefs = context.getSharedPreferences("BatteryWidgetPrefs", MODE_PRIVATE)
            val refreshInterval = prefs.getInt("refresh_interval_$appWidgetId", BatteryWidgetProvider.REFRESH_AUTO)

            // Only update widgets in AUTO mode
            if (refreshInterval == BatteryWidgetProvider.REFRESH_AUTO) {
                widgetProvider.updateWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    // =========================================================================
    // Service Lifecycle Methods
    // =========================================================================

    /**
     * Called when the service is created
     */
    override fun onCreate() {
        super.onCreate()
        Log.d("BatteryMonitorService", "Service created")

        // Register broadcast receiver for battery events
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(batteryReceiver, filter)
    }

    /**
     * Called when the service is destroyed
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d("BatteryMonitorService", "Service destroyed")
        // Unregister the broadcast receiver
        unregisterReceiver(batteryReceiver)
    }

    /**
     * Binding is not supported - return null
     */
    override fun onBind(intent: Intent): IBinder? = null

    /**
     * Called when the service is started
     *
     * @param intent The starting intent
     * @param flags Additional flags
     * @param startId Unique start ID
     * @return The start behavior flag
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BatteryMonitorService", "Service started")
        // Return START_STICKY to restart if killed by system
        return START_STICKY
    }
}