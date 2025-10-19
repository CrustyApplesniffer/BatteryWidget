package com.prometeo.batterywidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.prometeo.batterywidget.databinding.BatteryWidgetConfigureBinding

/**
 * Configuration Activity for Battery Widget
 *
 * Allows users to configure the refresh interval for the battery widget.
 * This activity is launched when the widget is first placed on the home screen
 * or when the user clicks on an existing widget.
 */
class BatteryWidgetConfigureActivity : AppCompatActivity() {

    // View binding instance
    private lateinit var binding: BatteryWidgetConfigureBinding

    // Widget ID being configured
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    /**
     * Called when the activity is first created
     *
     * @param savedInstanceState Previously saved instance state
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding
        binding = BatteryWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set result to CANCELED in case user backs out
        setResult(Activity.RESULT_CANCELED)

        // Extract widget ID from the intent
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        // Check if we're in widget configuration mode or informational mode
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            // Informational mode - opened from main activity
            setupInformationalMode()
        } else {
            // Configuration mode - opened for a specific widget
            setupConfigurationMode()
        }
    }

    // =========================================================================
    // UI Setup Methods
    // =========================================================================

    /**
     * Sets up the activity in informational mode (when no specific widget is being configured)
     * This mode is used when opening from the main activity
     */
    private fun setupInformationalMode() {
        binding.textViewTitle.text = getString(R.string.configure_widgets)
        binding.textViewInstructions.visibility = android.view.View.VISIBLE
        binding.textViewInstructions.text = getString(R.string.configure_default_text)

        setupRefreshIntervalSpinner()
        setupButtonsForInformationalMode()
    }

    /**
     * Sets up the activity in configuration mode (when a specific widget is being configured)
     * This mode is used when configuring an existing widget
     */
    private fun setupConfigurationMode() {
        binding.textViewTitle.text = getString(R.string.configure_widgets)
        binding.textViewInstructions.visibility = android.view.View.GONE

        setupRefreshIntervalSpinner()
        setupButtonsForConfigurationMode()
    }

    /**
     * Sets up the refresh interval spinner with available options
     */
    private fun setupRefreshIntervalSpinner() {
        val refreshOptions = arrayOf(
            "Auto (System)",  // Updates on battery change events
            "30 seconds",     // Frequent updates
            "1 minute",       // Balanced updates
            "15 minutes",     // Less frequent updates
            "30 minutes"      // Infrequent updates
        )

        // Create adapter for the spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, refreshOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRefreshInterval.adapter = adapter

        // Load current setting and set spinner selection
        val prefs = getSharedPreferences("BatteryWidgetPrefs", Context.MODE_PRIVATE)

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            // Informational mode - load default settings
            val currentInterval = prefs.getInt("default_refresh_interval", BatteryWidgetProvider.REFRESH_AUTO)
            val selectedPosition = when (currentInterval) {
                BatteryWidgetProvider.REFRESH_AUTO -> 0
                BatteryWidgetProvider.REFRESH_30_SEC -> 1
                BatteryWidgetProvider.REFRESH_1_MIN -> 2
                BatteryWidgetProvider.REFRESH_15_MIN -> 3
                BatteryWidgetProvider.REFRESH_30_MIN -> 4
                else -> 0
            }
            binding.spinnerRefreshInterval.setSelection(selectedPosition)
        } else {
            // Configuration mode - load specific widget settings
            val currentInterval = prefs.getInt("refresh_interval_$appWidgetId", BatteryWidgetProvider.REFRESH_AUTO)
            val selectedPosition = when (currentInterval) {
                BatteryWidgetProvider.REFRESH_AUTO -> 0
                BatteryWidgetProvider.REFRESH_30_SEC -> 1
                BatteryWidgetProvider.REFRESH_1_MIN -> 2
                BatteryWidgetProvider.REFRESH_15_MIN -> 3
                BatteryWidgetProvider.REFRESH_30_MIN -> 4
                else -> 0
            }
            binding.spinnerRefreshInterval.setSelection(selectedPosition)
        }
    }

    /**
     * Sets up button click listeners for informational mode
     */
    private fun setupButtonsForInformationalMode() {
        // Save button - saves default configuration
        binding.btnSave.text = getString(R.string.save_default_settings)
        binding.btnSave.setOnClickListener {
            saveDefaultConfiguration()
        }

        // Cancel button - closes activity without saving
        binding.btnCancel.setOnClickListener {
            finish()
        }

        // Remove the apply to all button for informational mode
        binding.btnApplyToAll.visibility = android.view.View.GONE
    }

    /**
     * Sets up button click listeners for configuration mode
     */
    private fun setupButtonsForConfigurationMode() {
        // Save button - saves configuration for specific widget
        binding.btnSave.text = getString(R.string.save_default_settings)
        binding.btnSave.setOnClickListener {
            saveConfiguration()
        }

        // Cancel button - closes activity without saving
        binding.btnCancel.setOnClickListener {
            finish()
        }

        // Apply to all button - applies settings to all existing widgets
        binding.btnApplyToAll.visibility = android.view.View.VISIBLE
        binding.btnApplyToAll.setOnClickListener {
            applyToAllWidgets()
        }
    }

    // =========================================================================
    // Configuration Methods
    // =========================================================================

    /**
     * Saves the default configuration for new widgets
     */
    private fun saveDefaultConfiguration() {
        // Determine selected interval based on spinner position
        val selectedInterval = when (binding.spinnerRefreshInterval.selectedItemPosition) {
            0 -> BatteryWidgetProvider.REFRESH_AUTO
            1 -> BatteryWidgetProvider.REFRESH_30_SEC
            2 -> BatteryWidgetProvider.REFRESH_1_MIN
            3 -> BatteryWidgetProvider.REFRESH_15_MIN
            4 -> BatteryWidgetProvider.REFRESH_30_MIN
            else -> BatteryWidgetProvider.REFRESH_AUTO
        }

        // Save default preference to SharedPreferences
        val prefs = getSharedPreferences("BatteryWidgetPrefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("default_refresh_interval", selectedInterval).apply()

        // Show confirmation message
        Toast.makeText(this, "Default settings saved!", Toast.LENGTH_SHORT).show()

        // Finish the activity
        finish()
    }

    /**
     * Saves the configuration for a specific widget and updates it
     */
    private fun saveConfiguration() {
        // Determine selected interval based on spinner position
        val selectedInterval = when (binding.spinnerRefreshInterval.selectedItemPosition) {
            0 -> BatteryWidgetProvider.REFRESH_AUTO
            1 -> BatteryWidgetProvider.REFRESH_30_SEC
            2 -> BatteryWidgetProvider.REFRESH_1_MIN
            3 -> BatteryWidgetProvider.REFRESH_15_MIN
            4 -> BatteryWidgetProvider.REFRESH_30_MIN
            else -> BatteryWidgetProvider.REFRESH_AUTO
        }

        // Save preference to SharedPreferences for this specific widget
        val prefs = getSharedPreferences("BatteryWidgetPrefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("refresh_interval_$appWidgetId", selectedInterval).apply()

        // Update the specific widget with new configuration
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val widgetProvider = BatteryWidgetProvider()
        widgetProvider.updateWidget(this, appWidgetManager, appWidgetId)

        // Set result and finish activity
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_OK, resultValue)

        // Show confirmation message
        Toast.makeText(this, "Widget settings saved!", Toast.LENGTH_SHORT).show()

        finish()
    }

    /**
     * Applies the current settings to all existing widgets
     */
    private fun applyToAllWidgets() {
        // Determine selected interval based on spinner position
        val selectedInterval = when (binding.spinnerRefreshInterval.selectedItemPosition) {
            0 -> BatteryWidgetProvider.REFRESH_AUTO
            1 -> BatteryWidgetProvider.REFRESH_30_SEC
            2 -> BatteryWidgetProvider.REFRESH_1_MIN
            3 -> BatteryWidgetProvider.REFRESH_15_MIN
            4 -> BatteryWidgetProvider.REFRESH_30_MIN
            else -> BatteryWidgetProvider.REFRESH_AUTO
        }

        // Apply to all existing widgets
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val widgetProvider = BatteryWidgetProvider()
        val thisWidget = android.content.ComponentName(this, BatteryWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

        val prefs = getSharedPreferences("BatteryWidgetPrefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        appWidgetIds.forEach { widgetId ->
            editor.putInt("refresh_interval_$widgetId", selectedInterval)
            widgetProvider.updateWidget(this, appWidgetManager, widgetId)
        }
        editor.apply()

        // Show confirmation message
        Toast.makeText(this, "Settings applied to ${appWidgetIds.size} widgets!", Toast.LENGTH_SHORT).show()
    }

    // =========================================================================
    // Companion Object for Intent Creation
    // =========================================================================

    companion object {
        /**
         * Creates an intent for opening the configuration activity
         *
         * @param context The application context
         * @param appWidgetId The widget ID to configure (use INVALID_APPWIDGET_ID for informational mode)
         * @return Intent configured to open the configuration activity
         */
        fun createConfigurationIntent(context: Context, appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID): Intent {
            return Intent(context, BatteryWidgetConfigureActivity::class.java).apply {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
            }
        }
    }
}