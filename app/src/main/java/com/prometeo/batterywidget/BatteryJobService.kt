package com.prometeo.batterywidget

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log

/**
 * Battery Job Service for Android 14+
 *
 * JobService implementation for battery monitoring on Android 14 and above.
 * Uses JobScheduler to work around background service restrictions while
 * providing real-time battery updates for widgets in AUTO mode.
 */
class BatteryJobService : JobService() {

    // =========================================================================
    // Class Properties
    // =========================================================================

    private var isReceiverRegistered = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    /**
     * BroadcastReceiver for battery-related intents
     *
     * Listens for battery change events and triggers widget updates
     * when the battery status changes.
     */
    private val batteryReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                Log.d("BatteryJobService", "Battery changed - updating widgets")
                updateAllWidgets(context)
            }
        }
    }

    // =========================================================================
    // JobService Lifecycle Methods
    // =========================================================================

    /**
     * Called when the job is started
     *
     * @param params Job parameters provided by the system
     * @return true if the job is ongoing, false if it has completed
     */
    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d("BatteryJobService", "✅ Job STARTED - registering battery receiver")
        registerBatteryReceiver()

        // Note: scheduleNextJob is commented out to prevent continuous rescheduling
        // Uncomment if periodic execution is desired
        // scheduleNextJob(this)

        return true // Job continues running in background
    }

    /**
     * Called when the job is stopped by the system
     *
     * @param params Job parameters provided by the system
     * @return true to reschedule the job, false to drop it
     */
    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d("BatteryJobService", "🛑 Job STOPPED - unregistering battery receiver")
        unregisterBatteryReceiver()

        // Reschedule the job if it was stopped
        scheduleNextJob(this)
        return true
    }

    // =========================================================================
    // Battery Monitoring Methods
    // =========================================================================

    /**
     * Registers the battery broadcast receiver
     *
     * Sets up intent filters for battery-related events and registers
     * the receiver to listen for these events.
     */
    private fun registerBatteryReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }
            registerReceiver(batteryReceiver, filter)
            isReceiverRegistered = true
            Log.d("BatteryJobService", "✅ Battery receiver REGISTERED")
        }
    }

    /**
     * Unregisters the battery broadcast receiver
     *
     * Cleans up the receiver when the job is stopped to prevent memory leaks
     * and unnecessary battery consumption.
     */
    private fun unregisterBatteryReceiver() {
        if (isReceiverRegistered) {
            unregisterReceiver(batteryReceiver)
            isReceiverRegistered = false
            Log.d("BatteryJobService", "✅ Battery receiver UNREGISTERED")
        }
    }

    /**
     * Schedules the next execution of this job
     *
     * @param context The application context used for scheduling the job
     */
    private fun scheduleNextJob(context: Context) {
        handler.postDelayed({
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    val jobScheduler = context.getSystemService(JOB_SCHEDULER_SERVICE) as android.app.job.JobScheduler

                    val jobInfo = android.app.job.JobInfo.Builder(1,
                        android.content.ComponentName(context, BatteryJobService::class.java))
                        .setRequiredNetworkType(android.app.job.JobInfo.NETWORK_TYPE_NONE)
                        .setPersisted(true)
                        .setMinimumLatency(1000) // 1 second
                        .setOverrideDeadline(5000) // 5 seconds maximum
                        .build()

                    jobScheduler.schedule(jobInfo)
                    Log.d("BatteryJobService", "✅ Next job SCHEDULED in 1-5s")
                } catch (e: Exception) {
                    Log.e("BatteryJobService", "❌ Error scheduling next job", e)
                }
            }
        }, 5000L) // Wait 5 seconds before rescheduling
    }

    /**
     * Updates all widgets with current battery information
     *
     * @param context The application context used for widget updates
     */
    private fun updateAllWidgets(context: Context) {
        try {
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
            val widgetProvider = BatteryWidgetProvider()
            val thisWidget = android.content.ComponentName(context, BatteryWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            if (appWidgetIds.isNotEmpty()) {
                appWidgetIds.forEach { appWidgetId ->
                    widgetProvider.updateWidget(context, appWidgetManager, appWidgetId)
                }
                Log.d("BatteryJobService", "✅ Updated ${appWidgetIds.size} widgets")
            } else {
                Log.d("BatteryJobService", "No widgets to update")
            }
        } catch (e: Exception) {
            Log.e("BatteryJobService", "❌ Error updating widgets", e)
        }
    }

    // =========================================================================
    // Unused Methods (Commented for future reference)
    // =========================================================================

    /*
    /**
     * Called when the service is created
     */
    override fun onCreate() {
        super.onCreate()
        Log.d("BatteryJobService", "🎯 JobService CREATED")
    }

    /**
     * Called when the service is destroyed
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d("BatteryJobService", "🎯 JobService DESTROYED")
    }
    */
}