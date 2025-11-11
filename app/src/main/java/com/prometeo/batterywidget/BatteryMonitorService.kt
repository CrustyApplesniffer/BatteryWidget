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

    // =========================================================================
    // Constants and Variables
    // =========================================================================

    private var lastUpdateTime = 0L
    private val minUpdateInterval = 2000L // Increased to 2 seconds
    private var isReceiverRegistered = false
    private var shouldRunForeground = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    companion object {
        private const val TAG = "BatteryMonitorService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "battery_widget_channel"

        /**
         * Starts the BatteryMonitorService
         * @param context The application context
         * @param foreground Whether to run as foreground service
         */
        fun startService(context: Context, foreground: Boolean = false) {
            val intent = Intent(context, BatteryMonitorService::class.java).apply {
                putExtra("foreground", foreground)
            }

            try {
                if (foreground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "Service start requested - Foreground: $foreground")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service", e)
            }
        }


        /**
         * Stops the BatteryMonitorService with logging
         * @param context The application context

        fun stopService(context: Context) {
            Log.d(TAG, "🛑 STOP SERVICE CALLED - Tracing who's stopping us...", Throwable("STOP SERVICE STACK TRACE"))
            val intent = Intent(context, BatteryMonitorService::class.java)
            context.stopService(intent)
        }
         */

        /**
         * Stops the service with additional tracking
         * @param context The application context

        fun stopServiceWithTracking(context: Context) {
            Log.d(TAG, "🛑 stopServiceWithTracking called", Throwable("DIRECT STOP TRACE"))
            stopService(context)
        }
         */

        /**
         * Tracks potential direct stop service calls
         * @param context The application context

        fun trackDirectStopService(context: Context) {
            Log.d(TAG, "🔍 SUSPECT: Direct stopService might be called soon", Throwable("POTENTIAL STOP SOURCE"))
        }
         */

        /**
         * Fallback method to update widgets directly when service fails
         * @param context The application context

        private fun updateWidgetsDirectly(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val widgetProvider = BatteryWidgetProvider()
                val thisWidget = ComponentName(context, BatteryWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

                appWidgetIds.forEach { appWidgetId ->
                    widgetProvider.updateWidget(context, appWidgetManager, appWidgetId)
                }
                Log.d(TAG, "Widgets updated directly via fallback")
            } catch (e: Exception) {
                Log.e(TAG, "Error in fallback widget update", e)
            }
        }
         */
    }

    // =========================================================================
    // Service Lifecycle Methods
    // =========================================================================

    /**
     * Service binding - this service doesn't support binding
     */
    override fun onBind(intent: Intent): IBinder? = null

    /**
     * BroadcastReceiver for battery-related intents
     */
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val currentTime = System.currentTimeMillis()

                // Improved anti-rebound mechanism to prevent too frequent updates
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

    /**
     * Updates widgets that are in AUTO refresh mode
     * @param context The application context
     */
    private fun updateWidgetsInAutoMode(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, BatteryWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            Log.d(TAG, "Found ${appWidgetIds.size} widgets in updateWidgetsInAutoMode")

            if (appWidgetIds.isEmpty()) {
                Log.d(TAG, "No widgets found, stopping service")
                stopServiceWithLogging("No widgets found")
                return
            }

            var hasAutoModeWidgets = false
            var updatedWidgets = 0

            appWidgetIds.forEach { appWidgetId ->
                val prefs = context.getSharedPreferences("BatteryWidgetPrefs", MODE_PRIVATE)
                val refreshInterval = prefs.getInt("refresh_interval_$appWidgetId", BatteryWidgetProvider.REFRESH_AUTO)

                // Only update widgets in AUTO mode
                if (refreshInterval == BatteryWidgetProvider.REFRESH_AUTO) {
                    hasAutoModeWidgets = true
                    BatteryWidgetProvider().updateWidget(context, appWidgetManager, appWidgetId)
                    updatedWidgets++
                }
            }

            Log.d(TAG, "Updated $updatedWidgets AUTO mode widgets, hasAutoModeWidgets: $hasAutoModeWidgets")

            // Only stop service if there are NO auto mode widgets at all
            if (!hasAutoModeWidgets) {
                Log.d(TAG, "No AUTO mode widgets found, stopping service")
                stopServiceWithLogging("No AUTO mode widgets")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error updating widgets", e)
        }
    }

    /**
     * Called when the service is created
     */
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel()
    }

    /**
     * Called when the service is started
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started with flags: $flags, startId: $startId")

        shouldRunForeground = intent?.getBooleanExtra("foreground", false) ?: false

        // Only start foreground if requested AND we're on a version that supports it
        if (shouldRunForeground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Log.d(TAG, "Starting foreground service")
            startForegroundService()
        } else if (shouldRunForeground) {
            Log.d(TAG, "Foreground requested but not supported on this API level, running in background")
            // On Android 14+, we run as background service but don't stop automatically
        }

        registerBatteryReceiver()
        scheduleKeepAlive()
        // Return START_STICKY to keep the service running and restart if killed
        //return START_STICKY
        return START_REDELIVER_INTENT
    }

    /**
     * Track when the service is being stopped externally
     */
    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind called", Throwable("onUnbind stack trace"))
        return super.onUnbind(intent)
    }

    /**
     * Called when the task associated with the service is removed
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "🔴 onTaskRemoved - System is removing our service", Throwable("onTaskRemoved Stack"))
        super.onTaskRemoved(rootIntent)
    }

    /**
     * Called when the system is running low on memory
     */
    override fun onLowMemory() {
        Log.d(TAG, "🔴 onLowMemory - System is low on memory")
        super.onLowMemory()
    }

    /**
     * Schedules a service restart after system kill
     */
    /*
    private fun scheduleRestart() {
        handler.postDelayed({
            Log.d(TAG, "Attempting to restart service after system kill")
            if (checkIfServiceShouldContinue()) {
                val intent = Intent(this, BatteryMonitorService::class.java)
                intent.putExtra("foreground", shouldRunForeground)
                startService(intent)
            }
        }, 5000L)
    }
    */

    /**
     * Called when the service is being destroyed
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called - checking who stopped us", Throwable("onDestroy stack trace"))

        Log.d(TAG, "Service onDestroy - shouldRunForeground: $shouldRunForeground, isReceiverRegistered: $isReceiverRegistered")

        // Log the current widget state
        try {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val thisWidget = ComponentName(this, BatteryWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            Log.d(TAG, "Widget count during onDestroy: ${appWidgetIds.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error checking widgets during onDestroy", e)
        }

        // Remove any pending keep-alive callbacks
        handler.removeCallbacksAndMessages(null)

        unregisterBatteryReceiver()
    }

    // =========================================================================
    // Service Management Methods
    // =========================================================================

    /**
     * Custom stop method that logs who's stopping the service
     * @param reason The reason for stopping the service
     */
    private fun stopServiceWithLogging(reason: String) {
        Log.d(TAG, "stopServiceWithLogging called - Reason: $reason", Throwable("Service stop stack trace"))
        stopSelf()
    }

    /**
     * Custom stop method with startId
     * @param startId The start ID of the service
     * @param reason The reason for stopping the service
     */
    private fun stopServiceWithLogging(startId: Int, reason: String) {
        Log.d(TAG, "stopServiceWithLogging called - StartId: $startId, Reason: $reason", Throwable("Service stop stack trace"))
        stopSelf(startId)
    }

    /**
     * Creates the notification channel for foreground service
     */
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

    /**
     * Checks if the service should continue running
     * @return true if service should continue, false otherwise
     */
    private fun checkIfServiceShouldContinue(): Boolean {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val thisWidget = ComponentName(this, BatteryWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            if (appWidgetIds.isEmpty()) {
                Log.d(TAG, "No widgets found - service should stop")
                return false
            }

            val prefs = getSharedPreferences("BatteryWidgetPrefs", MODE_PRIVATE)

            // Check if any widget is in AUTO mode
            val hasAutoModeWidget = appWidgetIds.any { appWidgetId ->
                val refreshInterval = prefs.getInt("refresh_interval_$appWidgetId", BatteryWidgetProvider.REFRESH_AUTO)
                refreshInterval == BatteryWidgetProvider.REFRESH_AUTO
            }

            Log.d(TAG, "Service continuation check: $hasAutoModeWidget (${appWidgetIds.size} widgets)")
            return hasAutoModeWidget

        } catch (e: Exception) {
            Log.e(TAG, "Error checking service continuation", e)
            return true // Default to continuing if we can't check
        }
    }

    /**
     * Prevents the service from being stopped by the system too quickly
     */
    private fun scheduleKeepAlive() {
        handler.postDelayed({
            if (!checkIfServiceShouldContinue()) {
                Log.d(TAG, "No reason to continue, stopping service")
                stopServiceWithLogging("Keep-alive check failed")
            } else {
                Log.d(TAG, "Service keep-alive check - should continue: true")
                scheduleKeepAlive() // Reschedule
            }
        }, 30000L) // Check every 30 seconds
    }

    /**
     * Starts the service in foreground mode with notification
     */
    private fun startForegroundService() {
        try {
            val notification = buildNotification()
            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "✅ Service started in foreground")
        }
        catch(e:Exception)
        {
            Log.e(TAG, "❌ Failed to start foreground service", e)
        }
    }

    /**
     * Builds the notification for foreground service
     * @return The built notification
     */
    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Battery Widget")
            .setContentText("Monitoring battery level changes")
            .setSmallIcon(R.drawable.ic_flash)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
    }

    /**
     * Registers the battery broadcast receiver
     */
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

    /**
     * Unregisters the battery broadcast receiver
     */
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