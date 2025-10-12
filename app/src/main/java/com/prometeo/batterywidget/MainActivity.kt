package com.prometeo.batterywidget

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.prometeo.batterywidget.databinding.ActivityMainBinding

/**
 * Main Activity for Battery Widget App
 *
 * Provides information about the app and instructions for using the widget.
 * This activity serves as the entry point when users launch the app from the launcher.
 */
class MainActivity : AppCompatActivity() {

    // View binding instance
    private lateinit var binding: ActivityMainBinding

    /**
     * Called when the activity is first created
     *
     * @param savedInstanceState Previously saved instance state
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding and set content view
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Display app information and usage instructions
        binding.textView.text = """
            Battery Widget App
            
            This app provides a home screen widget to display battery level.
            
            Features:
            • Real-time battery level monitoring
            • Dynamic color coding (Green → Yellow → Red)
            • Charging status indicator
            • Configurable update intervals
            • Auto-updates on battery changes
            • Tap widget to open battery settings
            
            To use:
            1. Go to your home screen
            2. Long press and select 'Widgets'
            3. Find 'Battery Widget'
            4. Drag to your home screen
            5. Configure update interval as needed
            
            The widget will automatically update based on your settings
            and show the current battery level with color-coded background.
        """.trimIndent()

        // Setup button click listeners
        setupButtons()
    }

    // =========================================================================
    // Button Setup Methods
    // =========================================================================

    /**
     * Sets up button click listeners for the main activity
     */
    private fun setupButtons() {
        // Configure Widgets Button - Opens configuration activity
        binding.btnConfigureWidgets.setOnClickListener {
            openConfigurationActivity()
        }

        // Open Battery Settings Button - Opens system battery settings
        binding.btnBatterySettings.setOnClickListener {
            openBatterySettings()
        }
    }

    // =========================================================================
    // Navigation Methods
    // =========================================================================

    /**
     * Opens the widget configuration activity
     * This allows users to configure default settings for new widgets
     */
    private fun openConfigurationActivity() {
        // Open in informational mode to configure default settings
        val intent = BatteryWidgetConfigureActivity.createConfigurationIntent(this)
        startActivity(intent)
    }

    /**
     * Opens the system battery settings screen
     * This provides access to system-level battery controls and information
     */
    private fun openBatterySettings() {
        try {
            // Primary option: Battery saver settings (Android 5.0+)
            val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            try {
                // Fallback 1: Power usage settings
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
            } catch (e2: Exception) {
                try {
                    // Fallback 2: General settings
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    startActivity(intent)
                } catch (e3: Exception) {
                    // Final fallback: Open app info
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                }
            }
        }
    }
}