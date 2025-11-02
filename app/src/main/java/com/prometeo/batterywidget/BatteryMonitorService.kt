package com.prometeo.batterywidget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Battery Monitoring Service
 *
 * Background service that listens for battery change events and updates
 * widgets in AUTO mode in real-time. This ensures immediate updates when
 * battery status changes without relying on periodic polling.
 */
class BatteryMonitorService : Service() {

    private var lastUpdateTime = 0L
    private val minUpdateInterval = 2000L // Increased to 2 seconds
    private var isReceiverRegistered = false
    private var shouldRunForeground = false

    companion object {
        private const val TAG = "BatteryMonitorService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "battery_widget_channel"

        fun startService(context: Context, foreground: Boolean = false) {
            val intent = Intent(context, BatteryMonitorService::class.java).apply {
                putExtra("foreground", foreground)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, BatteryMonitorService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent): IBinder? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val currentTime = System.currentTimeMillis()

                // Improved anti-rebound
                if (currentTime - lastUpdateTime < minUpdateInterval) {
                    return
                }
                lastUpdateTime = currentTime

                when (intent.action) {
                    Intent.ACTION_BATTERY_CHANGED,
                    Intent.ACTION_POWER_CONNECTED,
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        Log.d(TAG, "Battery event: ${intent.action}")
                        updateWidgetsInAutoMode(context)
                    }
                    // Ignore BATTERY_LOW and BATTERY_OKAY to reduce updates
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in battery receiver", e)
            }
        }
    }

    private fun updateWidgetsInAutoMode(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, BatteryWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            if (appWidgetIds.isEmpty()) {
                Log.d(TAG, "No widgets found, stopping service")
                stopSelf()
                return
            }

            appWidgetIds.forEach { appWidgetId ->
                val prefs = context.getSharedPreferences("BatteryWidgetPrefs", MODE_PRIVATE)
                val refreshInterval = prefs.getInt("refresh_interval_$appWidgetId", BatteryWidgetProvider.REFRESH_AUTO)

            // Only update widgets in AUTO mode
                if (refreshInterval == BatteryWidgetProvider.REFRESH_AUTO) {
                    BatteryWidgetProvider().updateWidget(context, appWidgetManager, appWidgetId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widgets", e)
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
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started with flags: $flags, startId: $startId")

        shouldRunForeground = intent?.getBooleanExtra("foreground", false) ?: false

        if (shouldRunForeground) {
            startForegroundService()
        }

        registerBatteryReceiver()

        // Pour Android 14+, utiliser START_NOT_STICKY
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            START_NOT_STICKY
        } else {
            START_STICKY
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        unregisterBatteryReceiver()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Battery Widget Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows battery level updates for home screen widgets"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "Started in foreground mode")
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Battery Widget")
            .setContentText("Monitoring battery level changes")
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
    }

    private fun registerBatteryReceiver() {
        if (!isReceiverRegistered) {
            try {
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_BATTERY_CHANGED)
                    addAction(Intent.ACTION_POWER_CONNECTED)
                    addAction(Intent.ACTION_POWER_DISCONNECTED)
                    // Reducing the number of actions to improve performance
                }
                registerReceiver(batteryReceiver, filter)
                isReceiverRegistered = true
                Log.d(TAG, "Battery receiver registered for limited actions")
            } catch (e: Exception) {
                Log.e(TAG, "Error registering battery receiver", e)
            }
        }
    }

    private fun unregisterBatteryReceiver() {
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(batteryReceiver)
                isReceiverRegistered = false
                Log.d(TAG, "Battery receiver unregistered")
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering battery receiver", e)
            }
        }
    }
}