package com.prometeo.batterywidget

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.prometeo.batterywidget.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


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

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set scrollable text content with real values
        binding.textView.text = createAppInfoText()

        setupButtons()
    }

    private fun createAppInfoText(): String {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = packageInfo.versionName
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }

        val buildDate = getBuildDate()
        val minSdk = getMinSdkVersion()
        val targetSdk = getTargetSdkVersion()
        val compileSdk = getCompileSdkVersion()

        return """
            Battery Widget - Complete Guide
            
            Welcome to Battery Widget! This app provides a beautiful, functional battery monitoring widget for your home screen.

            📊 Features Overview:

            Real-time Monitoring
            • Always see your current battery level
            • Live updates with accurate percentage
            • No delays or lag in battery information

            🎨 Visual Design
            • Dynamic color coding:
              - Green (80-100%): High battery
              - Light Green (60-79%): Good battery  
              - Yellow (40-59%): Medium battery
              - Orange (20-39%): Low battery
              - Red (0-19%): Critical battery
            • Clean, minimalist design
            • Smooth progress bar animation

            ⚡ Smart Functionality
            • Charging status indicator (flash icon)
            • Auto-updates when battery changes
            • Manual refresh intervals available
            • Tap widget to open battery settings

            ⚙️ Configuration Options
            • Auto Mode: Updates on battery changes
            • 30 Seconds: Frequent updates
            • 1 Minute: Balanced updates
            • 15 Minutes: Less frequent updates
            • 30 Minutes: Power-saving updates

            🏠 Installation & Usage

            Adding the Widget:
            1. Long-press on your home screen
            2. Select "Widgets" from the menu
            3. Scroll to find "Battery Widget"
            4. Drag and drop to your desired location
            5. The widget will automatically start working

            Configuring the Widget:
            • Tap the widget to open configuration
            • Choose your preferred update interval
            • Settings are saved automatically

            🔋 Battery Settings Access
            One tap on the widget opens your device's battery settings, where you can:
            • View detailed battery usage
            • Enable battery saver mode
            • Check which apps use most power
            • Optimize battery settings

            🔒 Privacy & Permissions
            • No internet connection required
            • No personal data collection
            • Only reads battery level and charging status
            • All data stays on your device
            • No ads or tracking

            📱 App Information
            
            Version Information:
            • App Version: $versionName (Build $versionCode)
            • Minimum Android: ${getAndroidVersionName(minSdk)} (API $minSdk)
            • Target Android: ${getAndroidVersionName(targetSdk)} (API $targetSdk)
            • Compiled with Android SDK: ${getAndroidVersionName(compileSdk)} (API $compileSdk)
            • Last Updated: $buildDate

            Device Information:
            • Current Android: ${getAndroidVersionName(Build.VERSION.SDK_INT)} (API ${Build.VERSION.SDK_INT})
            • Device: ${Build.MANUFACTURER} ${Build.MODEL}

            ❓ Troubleshooting

            Common Issues:
            • Widget not updating: Check refresh interval settings
            • Wrong battery level: Restart the widget or device
            • Charging icon not showing: Replug charger or check connection

            Need Help?
            If you encounter any issues or have suggestions for improvement, please contact us through the app store listing.

            Thank you for choosing Battery Widget! We hope this app helps you better monitor and manage your device's battery life.
        """.trimIndent()
    }

    private fun getBuildDate(): String {
        return try {
            val buildTime = BuildConfig.BUILD_TIME.toLong()
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm", Locale.getDefault())
            dateFormat.format(Date(buildTime))
        } catch (e: Exception) {
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            dateFormat.format(Date())
        }
    }

    private fun getMinSdkVersion(): Int {
        return try {
            BuildConfig.MIN_SDK_VERSION
        } catch (e: Exception) {
            // Fallback
            Build.VERSION_CODES.LOLLIPOP
        }
    }

    private fun getTargetSdkVersion(): Int {
        return try {
            BuildConfig.TARGET_SDK_VERSION
        } catch (e: Exception) {
            Build.VERSION.SDK_INT
        }
    }

    private fun getCompileSdkVersion(): Int {
        return try {
            BuildConfig.COMPILE_SDK_VERSION
        } catch (e: Exception) {
            34 // Fallback to your actual compileSdk
        }
    }

    private fun getAndroidVersionName(apiLevel: Int): String {
        return when (apiLevel) {
            Build.VERSION_CODES.BASE -> "Android 1.0"
            Build.VERSION_CODES.BASE_1_1 -> "Android 1.1"
            Build.VERSION_CODES.CUPCAKE -> "Android 1.5 (Cupcake)"
            Build.VERSION_CODES.DONUT -> "Android 1.6 (Donut)"
            Build.VERSION_CODES.ECLAIR -> "Android 2.0 (Eclair)"
            Build.VERSION_CODES.ECLAIR_0_1 -> "Android 2.0.1 (Eclair)"
            Build.VERSION_CODES.ECLAIR_MR1 -> "Android 2.1 (Eclair)"
            Build.VERSION_CODES.FROYO -> "Android 2.2 (Froyo)"
            Build.VERSION_CODES.GINGERBREAD -> "Android 2.3 (Gingerbread)"
            Build.VERSION_CODES.GINGERBREAD_MR1 -> "Android 2.3.3 (Gingerbread)"
            Build.VERSION_CODES.HONEYCOMB -> "Android 3.0 (Honeycomb)"
            Build.VERSION_CODES.HONEYCOMB_MR1 -> "Android 3.1 (Honeycomb)"
            Build.VERSION_CODES.HONEYCOMB_MR2 -> "Android 3.2 (Honeycomb)"
            Build.VERSION_CODES.ICE_CREAM_SANDWICH -> "Android 4.0 (Ice Cream Sandwich)"
            Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1 -> "Android 4.0.3 (Ice Cream Sandwich)"
            Build.VERSION_CODES.JELLY_BEAN -> "Android 4.1 (Jelly Bean)"
            Build.VERSION_CODES.JELLY_BEAN_MR1 -> "Android 4.2 (Jelly Bean)"
            Build.VERSION_CODES.JELLY_BEAN_MR2 -> "Android 4.3 (Jelly Bean)"
            Build.VERSION_CODES.KITKAT -> "Android 4.4 (KitKat)"
            Build.VERSION_CODES.KITKAT_WATCH -> "Android 4.4W (KitKat Wear)"
            Build.VERSION_CODES.LOLLIPOP -> "Android 5.0 (Lollipop)"
            Build.VERSION_CODES.LOLLIPOP_MR1 -> "Android 5.1 (Lollipop)"
            Build.VERSION_CODES.M -> "Android 6.0 (Marshmallow)"
            Build.VERSION_CODES.N -> "Android 7.0 (Nougat)"
            Build.VERSION_CODES.N_MR1 -> "Android 7.1 (Nougat)"
            Build.VERSION_CODES.O -> "Android 8.0 (Oreo)"
            Build.VERSION_CODES.O_MR1 -> "Android 8.1 (Oreo)"
            Build.VERSION_CODES.P -> "Android 9.0 (Pie)"
            Build.VERSION_CODES.Q -> "Android 10"
            Build.VERSION_CODES.R -> "Android 11"
            Build.VERSION_CODES.S -> "Android 12"
            Build.VERSION_CODES.S_V2 -> "Android 12L"
            Build.VERSION_CODES.TIRAMISU -> "Android 13"
            Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> "Android 14"
            else -> {
                if (apiLevel > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    "Android ${apiLevel} (Future Version)"
                } else {
                    "Android (API $apiLevel)"
                }
            }
        }
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