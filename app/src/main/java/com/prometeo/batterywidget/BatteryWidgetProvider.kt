package com.prometeo.batterywidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.provider.Settings
import android.widget.RemoteViews
import kotlinx.coroutines.*

/**
 * Battery Widget Provider
 *
 * Main AppWidgetProvider class that handles battery widget updates and lifecycle.
 * Displays current battery level with dynamic background colors and charging status.
 *
 * Features:
 * - Real-time battery level monitoring
 * - Dynamic background color based on battery level
 * - Charging status indicator
 * - Configurable refresh intervals
 * - Auto-update on battery changes
 * - Tap to open battery settings
 */
class BatteryWidgetProvider : AppWidgetProvider() {

    // Coroutine scope for handling background updates
    private val scope = CoroutineScope(Dispatchers.Main)
    private var updateJob: Job? = null

    // Refresh interval constants
    companion object {
        /** Auto mode - updates on battery change events */
        const val REFRESH_AUTO = 0

        /** 30 seconds refresh interval */
        const val REFRESH_30_SEC = 30000

        /** 1 minute refresh interval */
        const val REFRESH_1_MIN = 60000

        /** 15 minutes refresh interval */
        const val REFRESH_15_MIN = 900000

        /** 30 minutes refresh interval */
        const val REFRESH_30_MIN = 1800000

        /** Fallback update interval for AUTO mode (5 minutes) to ensure data freshness */
        private const val FALLBACK_UPDATE_INTERVAL = 300000L
    }

    /**
     * Data class to hold battery status information
     *
     * @property level Current battery level (0-100)
     * @property isCharging Whether the device is currently charging
     */
    data class BatteryStatus(val level: Int, val isCharging: Boolean)

    // =========================================================================
    // AppWidgetProvider Lifecycle Methods
    // =========================================================================

    /**
     * Called when the widget needs to be updated
     *
     * @param context The application context
     * @param appWidgetManager The AppWidgetManager instance
     * @param appWidgetIds Array of widget IDs that need updating
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update each widget individually
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }

        // Start the monitoring service when widgets are active
        startBatteryMonitoringService(context)
    }

    /**
     * Called when the widget is first placed on the home screen
     *
     * @param context The application context
     */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Start service when first widget is created
        startBatteryMonitoringService(context)
    }

    /**
     * Called when the last widget instance is removed from the home screen
     *
     * @param context The application context
     */
    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        updateJob?.cancel()
        scope.cancel()

        // Stop service when last widget is removed
        context?.let {
            stopBatteryMonitoringService(it)
        }
    }

    /**
     * Called when one or more widgets are deleted
     *
     * @param context The application context
     * @param appWidgetIds Array of widget IDs that were deleted
     */
    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        updateJob?.cancel()

        // Clean up preferences when widget is deleted
        context?.let {
            val prefs = it.getSharedPreferences("BatteryWidgetPrefs", Context.MODE_PRIVATE)
            appWidgetIds?.forEach { appWidgetId ->
                prefs.edit().remove("refresh_interval_$appWidgetId").apply()
            }

            // Check if any widgets remain, if not stop service
            val appWidgetManager = AppWidgetManager.getInstance(it)
            val thisWidget = ComponentName(it, BatteryWidgetProvider::class.java)
            val remainingAppWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            if (remainingAppWidgetIds.isEmpty()) {
                stopBatteryMonitoringService(it)
            }
        }
    }

    /**
     * Called when the widget size changes
     *
     * @param context The application context
     * @param appWidgetManager The AppWidgetManager instance
     * @param appWidgetId The widget ID that changed
     * @param newOptions The new widget options
     */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle?
    ) {
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    /**
     * Called when broadcast intents are received
     *
     * @param context The application context
     * @param intent The received intent
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        // Handle battery change broadcasts for AUTO mode
        if (context != null && intent?.action == Intent.ACTION_BATTERY_CHANGED) {
            updateAllWidgetsForAutoMode(context)
        }
    }

    // =========================================================================
    // Service Management Methods
    // =========================================================================

    /**
     * Starts the battery monitoring service for real-time updates
     *
     * @param context The application context
     */
    private fun startBatteryMonitoringService(context: Context) {
        val serviceIntent = Intent(context, BatteryMonitorService::class.java)
        context.startService(serviceIntent)
    }

    /**
     * Stops the battery monitoring service
     *
     * @param context The application context
     */
    private fun stopBatteryMonitoringService(context: Context) {
        val serviceIntent = Intent(context, BatteryMonitorService::class.java)
        context.stopService(serviceIntent)
    }

    // =========================================================================
    // Widget Update Methods
    // =========================================================================

    /**
     * Updates all widgets that are set to AUTO refresh mode
     *
     * @param context The application context
     */
    private fun updateAllWidgetsForAutoMode(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, BatteryWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

        appWidgetIds.forEach { appWidgetId ->
            val prefs = context.getSharedPreferences("BatteryWidgetPrefs", Context.MODE_PRIVATE)
            val refreshInterval = prefs.getInt("refresh_interval_$appWidgetId", REFRESH_AUTO)

            // Only update if this widget is in AUTO mode
            if (refreshInterval == REFRESH_AUTO) {
                updateWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    /**
     * Main widget update method - updates the widget UI with current battery status
     *
     * @param context The application context
     * @param appWidgetManager The AppWidgetManager instance
     * @param appWidgetId The widget ID to update
     */
    fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.battery_widget)

        // Get current battery information
        val batteryStatus = getBatteryStatus(context)
        val batteryLevel = batteryStatus.level
        val isCharging = batteryStatus.isCharging

        // Update battery percentage text
        views.setTextViewText(R.id.battery_text, "$batteryLevel%")

        // Update progress bar
        views.setProgressBar(R.id.battery_progress, 100, batteryLevel, false)

        // Update charging icon visibility
        if (isCharging) {
            views.setImageViewResource(R.id.charging_icon, R.drawable.ic_flash)
            views.setViewVisibility(R.id.charging_icon, android.view.View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.charging_icon, android.view.View.GONE)
        }

        // Update background color based on battery level
        val backgroundColor = getBatteryColor(batteryLevel)
        views.setInt(R.id.widget_layout, "setBackgroundColor", backgroundColor)

        // =========================================================================
        // TAP BEHAVIOR: Open battery settings when widget is tapped
        // =========================================================================

        // Set click intent to open BATTERY SETTINGS

        val batterySettingsIntent = createBatterySettingsIntent(context)
        val batterySettingsPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            batterySettingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_layout, batterySettingsPendingIntent)


        // Apply the updates to the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)

        // Schedule the next update based on refresh interval
        scheduleNextUpdate(context, appWidgetManager, appWidgetId)
    }

    // =========================================================================
    // Intent Creation Methods
    // =========================================================================

    /**
     * Creates an intent to open battery settings
     * Uses multiple fallback options for different Android versions
     *
     * @param context The application context
     * @return Intent configured to open battery settings
     */
    private fun createBatterySettingsIntent(context: Context): Intent {
        return try {
            // Primary option: Battery saver settings (Android 5.0+)
            Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } catch (e: Exception) {
            try {
                // Fallback 1: Power usage settings
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            } catch (e: Exception) {
                // Fallback 2: General settings
                Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            }
        }
    }

    // =========================================================================
    // Battery Status & Color Methods
    // =========================================================================

    /**
     * Retrieves current battery status from the system
     *
     * @param context The application context
     * @return BatteryStatus object containing level and charging state
     */
    private fun getBatteryStatus(context: Context): BatteryStatus {
        val batteryIntent = context.registerReceiver(null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        // Extract battery information from the intent
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        // Calculate battery percentage
        val batteryPct = if (level != -1 && scale != -1) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            -1
        }

        return BatteryStatus(batteryPct, isCharging)
    }

    /**
     * Determines background color based on battery level
     * Uses a color gradient from green (full) to red (empty)
     *
     * @param batteryLevel Current battery level (0-100)
     * @return Color integer for the widget background
     */
    private fun getBatteryColor(batteryLevel: Int): Int {
        return when {
            batteryLevel >= 80 -> Color.parseColor("#FF4CAF50") // Green - High
            batteryLevel >= 60 -> Color.parseColor("#FF8BC34A") // Light Green - Good
            batteryLevel >= 40 -> Color.parseColor("#FFFFC107") // Amber/Yellow - Medium
            batteryLevel >= 20 -> Color.parseColor("#FFFF9800") // Orange - Low
            else -> Color.parseColor("#FFF44336") // Red - Critical
        }
    }

    // =========================================================================
    // Scheduling Methods
    // =========================================================================

    /**
     * Schedules the next widget update based on the configured refresh interval
     *
     * @param context The application context
     * @param appWidgetManager The AppWidgetManager instance
     * @param appWidgetId The widget ID to schedule updates for
     */
    private fun scheduleNextUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        updateJob?.cancel()

        // Use the new method that supports default settings
        val refreshInterval = getRefreshInterval(context, appWidgetId)

        if (refreshInterval != REFRESH_AUTO) {
            // For manual intervals, schedule the next update
            updateJob = scope.launch {
                delay(refreshInterval.toLong())
                updateWidget(context, appWidgetManager, appWidgetId)
            }
        } else {
            // For AUTO mode, we rely on battery change broadcasts
            // But also schedule a fallback update to ensure data freshness
            updateJob = scope.launch {
                delay(FALLBACK_UPDATE_INTERVAL)
                updateWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    /**
     * Gets the refresh interval for a widget, using default settings if not configured
     *
     * @param context The application context
     * @param appWidgetId The widget ID
     * @return The refresh interval in milliseconds
     */
    private fun getRefreshInterval(context: Context, appWidgetId: Int): Int {
        val prefs = context.getSharedPreferences("BatteryWidgetPrefs", Context.MODE_PRIVATE)

        // First try to get widget-specific setting
        val widgetInterval = prefs.getInt("refresh_interval_$appWidgetId", -1)
        if (widgetInterval != -1) {
            return widgetInterval
        }

        // If no widget-specific setting, use default
        return prefs.getInt("default_refresh_interval", REFRESH_AUTO)
    }
}