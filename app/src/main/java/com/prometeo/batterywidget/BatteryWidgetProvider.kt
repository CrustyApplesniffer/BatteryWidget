package com.prometeo.batterywidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.widget.RemoteViews
import androidx.core.graphics.toColorInt
import androidx.core.content.edit
import kotlinx.coroutines.*
import android.util.Log
import androidx.annotation.RequiresApi

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

    // =========================================================================
    // Properties and Constants
    // =========================================================================

    /** Coroutine scope for handling background updates */
    private val scope = CoroutineScope(Dispatchers.Main)

    /** Current update job */
    private var updateJob: Job? = null

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
        Log.d("BatteryWidgetProvider", "onUpdate called for ${appWidgetIds.size} widgets")

        // Update each widget individually
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }

        // Handling varies depending on the Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startBatteryMonitoringForAndroid14(context)
        } else {
            startBatteryMonitoringService(context)
        }
    }

    /**
     * Called when the widget is first placed on the home screen
     *
     * @param context The application context
     */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d("BatteryWidgetProvider", "First widget enabled")

        // Start service when first widget is created
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Log.d("BatteryWidgetProvider", "Scheduling JobService for Android 14+")
            startBatteryMonitoringForAndroid14(context)
        } else {
            startBatteryMonitoringService(context)
        }
    }

    /**
     * Called when the last widget instance is removed from the home screen
     *
     * @param context The application context
     */
    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        Log.d("BatteryWidgetProvider", "Last widget disabled")
        updateJob?.cancel()
        scope.cancel()

    }

    /**
     * Called when one or more widgets are deleted
     *
     * @param context The application context
     * @param appWidgetIds Array of widget IDs that were deleted
     */
    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        //BatteryMonitorService.amITheStopper(context!!, "BatteryWidgetProvider.onDeleted")
        Log.d("BatteryWidgetProvider", "Widgets deleted: ${appWidgetIds?.size}")

        updateJob?.cancel()

        // Clean up preferences when widget is deleted
        context?.let {
            val prefs = it.getSharedPreferences("BatteryWidgetPrefs", Context.MODE_PRIVATE)
            prefs.edit {
                appWidgetIds?.forEach { appWidgetId ->
                    remove("refresh_interval_$appWidgetId")
                }
            }

            // Use a delayed check to avoid race conditions
            //handler.postDelayed({
            //    checkAndStopServiceIfNeeded(it)
            //}, 1000L) // Wait 1 second for the system to process the deletion
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
     * Starts the battery monitoring service for real-time updates (Android <12)
     *
     * @param context The application context
     */
    private fun startBatteryMonitoringService(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ - Use optimized approach
                startBatteryMonitoringForAndroid14(context)
            } else {
                // Older Android versions - Use standard service
                val useForeground = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                BatteryMonitorService.startService(context, useForeground)
                Log.d("BatteryWidgetProvider", "Service started for older Android")
            }
        } catch (e: Exception) {
            Log.e("BatteryWidgetProvider", "Error starting service", e)
            updateAllWidgetsForAutoMode(context)
        }
    }

    /**
     * Starts battery monitoring for Android 14+ using JobScheduler
     *
     * @param context The application context
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun startBatteryMonitoringForAndroid14(context: Context) {
        Log.d("BatteryWidgetProvider", "Using JobScheduler for Android 14+")

        if (!hasAutoModeWidgets(context)) {
            Log.d("BatteryWidgetProvider", "No AUTO mode widgets, skipping monitoring")
            return
        }

        // Use JobScheduler instead of standard service
        if (scheduleBatteryJob(context)) {
            Log.d("BatteryWidgetProvider", "✅ JobScheduler SUCCESS - job should start in 1-5s")
        } else {
            Log.e("BatteryWidgetProvider", "❌ JobScheduler FAILED, using fallback service")
            tryStartServiceOnce(context)
        }
    }

    /**
     * Schedules a battery monitoring job using JobScheduler
     *
     * @param context The application context
     * @return true if job scheduling was successful, false otherwise
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun scheduleBatteryJob(context: Context): Boolean {
        try {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as android.app.job.JobScheduler

            // Cancel any existing job
            jobScheduler.cancel(1)

            val jobInfo = android.app.job.JobInfo.Builder(1,
                ComponentName(context, BatteryJobService::class.java)
            )
                .setRequiredNetworkType(android.app.job.JobInfo.NETWORK_TYPE_NONE)
                .setPersisted(true) // Survives reboots
                .setMinimumLatency(1000) // 1 second
                .setOverrideDeadline(5000) // 5 seconds maximum
                .build()

            val result = jobScheduler.schedule(jobInfo)
            val success = result == android.app.job.JobScheduler.RESULT_SUCCESS
            Log.d("BatteryWidgetProvider", "Job scheduling result: $success")
            return success
        } catch (e: Exception) {
            Log.e("BatteryWidgetProvider", "Error scheduling JobService", e)
            return false
        }
    }

    /**
     * Attempts to start the service once as a fallback
     *
     * @param context The application context
     */
    private fun tryStartServiceOnce(context: Context) {
        try {
            // Try to start service once (may work if app is in foreground)
            BatteryMonitorService.startService(context, false)
            Log.d("BatteryWidgetProvider", "Service started as fallback")
        } catch (e: Exception) {
            Log.e("BatteryWidgetProvider", "Fallback service also failed", e)
        }
    }

    /**
     * Check if any widgets are in AUTO mode
     *
     * @param context The application context
     * @return true if any widget is in AUTO mode, false otherwise
     */
    private fun hasAutoModeWidgets(context: Context): Boolean {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, BatteryWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            if (appWidgetIds.isEmpty()) {
                return false
            }

            val prefs = context.getSharedPreferences("BatteryWidgetPrefs", Context.MODE_PRIVATE)

            // Check if any widget is in AUTO mode
            return appWidgetIds.any { appWidgetId ->
                val refreshInterval = prefs.getInt("refresh_interval_$appWidgetId", REFRESH_AUTO)
                refreshInterval == REFRESH_AUTO
            }
        } catch (e: Exception) {
            Log.e("BatteryWidgetProvider", "Error checking AUTO mode widgets", e)
            return false
        }
    }

    /**
     * Starts temporary service for Android 12+
     *
     * @param context The application context
     */
    /*
    private fun startTemporaryService(context: Context) {
        try {
            startBatteryMonitoringService(context)
        } catch (e: Exception) {
            Log.e("BatteryWidgetProvider", "Error starting temporary service", e)
        }
    }
    */

    /**
     * Stops the battery monitoring service
     *
     * @param context The application context
     */
    /*
    private fun stopBatteryMonitoringService(context: Context) {
        try {
            Log.d("BatteryWidgetProvider", "stopBatteryMonitoringService called from:", Throwable("BatteryWidgetProvider stop stack trace"))
            BatteryMonitorService.stopService(context)
            Log.d("BatteryWidgetProvider", "Service stop requested")
        } catch (e: Exception) {
            Log.e("BatteryWidgetProvider", "Error stopping service", e)
        }
    }
    */

    /**
     * Check if any widgets remain and stop service only if truly no widgets left
     *
     * @param context The application context
     */
    /*
    private fun checkAndStopServiceIfNeeded(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, BatteryWidgetProvider::class.java)
            val remainingAppWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            Log.d("BatteryWidgetProvider", "DELETION CHECK: Remaining widgets: ${remainingAppWidgetIds.size}")

            // Only stop if ABSOLUTELY sure - be very conservative
            if (remainingAppWidgetIds.isEmpty()) {
                Log.d("BatteryWidgetProvider", "CONFIRMED: No widgets remaining after deletion")

                // Double-check after another delay to be absolutely sure
                handler.postDelayed({
                    val finalCheck = appWidgetManager.getAppWidgetIds(thisWidget)
                    if (finalCheck.isEmpty()) {
                        Log.d("BatteryWidgetProvider", "FINAL CONFIRMATION: Stopping service - no widgets")
                        //stopBatteryMonitoringService(context)
                    } else {
                        Log.d("BatteryWidgetProvider", "Widgets reappeared, keeping service running")
                    }
                }, 3000L) // Wait 3 seconds for final confirmation
            } else {
                Log.d("BatteryWidgetProvider", "Widgets still exist, keeping service running")

                // Log which widgets are still present and their modes
                remainingAppWidgetIds.forEach { appWidgetId ->
                    val prefs = context.getSharedPreferences("BatteryWidgetPrefs", Context.MODE_PRIVATE)
                    val refreshInterval = prefs.getInt("refresh_interval_$appWidgetId", REFRESH_AUTO)
                    Log.d("BatteryWidgetProvider", "Widget $appWidgetId - Mode: $refreshInterval")
                }
            }
        } catch (e: Exception) {
            Log.e("BatteryWidgetProvider", "Error in widget deletion check", e)
        }
    }
    */

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

        Log.d("BatteryWidgetProvider", "Updating ${appWidgetIds.size} auto-mode widgets")

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
        try {
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

            val batterySettingsIntent = createBatterySettingsIntent()
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

            Log.d("BatteryWidgetProvider", "Widget $appWidgetId updated - Battery: $batteryLevel%")

        } catch (e: Exception) {
            Log.e("BatteryWidgetProvider", "Error updating widget $appWidgetId", e)
        }
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
    private fun createBatterySettingsIntent(): Intent {
        return try {
            // Primary option: Battery settings (Android 5.0+)
            Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } catch (e: Exception) {
            Log.w("BatteryWidgetProvider", "FAILED to create battery settings intent", e)
            try {
                // Fallback 1: Power usage settings
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            } catch (e: Exception) {
                Log.w("BatteryWidgetProvider", "FAILED to create Power usage settings intent", e)
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
        return try {
            when {
                batteryLevel >= 80 -> "#FF4CAF50".toColorInt() // Green - High
                batteryLevel >= 60 -> "#FF8BC34A".toColorInt() // Light Green - Good
                batteryLevel >= 40 -> "#FFFFC107".toColorInt() // Amber/Yellow - Medium
                batteryLevel >= 20 -> "#FFFF9800".toColorInt() // Orange - Low
                else -> "#FFF44336".toColorInt() // Red - Critical
            }
        } catch (e: IllegalArgumentException) {
            Log.e("BatteryWidgetProvider", "Invalid color format, using default gray", e)
            "#FF666666".toColorInt() // Fallback gray
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

        updateJob = if (refreshInterval != REFRESH_AUTO) {
            // For manual intervals, schedule the next update
            scope.launch {
                delay(refreshInterval.toLong())
                updateWidget(context, appWidgetManager, appWidgetId)
            }
        } else {
            // For AUTO mode, we rely on battery change broadcasts
            // But also schedule a fallback update to ensure data freshness
            scope.launch {
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