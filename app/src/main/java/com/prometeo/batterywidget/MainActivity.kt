package com.prometeo.batterywidget

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.prometeo.batterywidget.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main Activity for Battery Widget App
 *
 * Provides information about the app and instructions for using the widget.
 * This activity serves as the entry point when users launch the app from the launcher.
 * Displays comprehensive app information, device details, and provides navigation
 * to widget configuration and system battery settings.
 */
class MainActivity : AppCompatActivity() {

    // =========================================================================
    // Class Properties
    // =========================================================================

    private lateinit var binding: ActivityMainBinding

    // =========================================================================
    // Activity Lifecycle Methods
    // =========================================================================

    /**
     * Called when the activity is first created
     *
     * @param savedInstanceState Previously saved instance state, null if fresh start
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "onCreate")

        // Configure system window behavior
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)

        // Set app info text before setting content view to avoid warnings
        binding.textView.text = createAppInfoText()

        // Set the content view
        setContentView(binding.root)

        // Apply window insets for proper system bar handling
        applyWindowInsets()

        // Set up button click listeners
        setupButtons()
    }

    /**
     * Called when the activity is being destroyed
     */
    //override fun onDestroy() {
    //    // Debug logging to track service stops
    //    BatteryMonitorService.amITheStopper(this, "MainActivity.onDestroy")
    //    super.onDestroy()
    //}

    // =========================================================================
    // UI Setup Methods
    // =========================================================================

    /**
     * Applies window insets to handle system bars properly
     *
     * Ensures content doesn't overlap with status and navigation bars
     * by adding appropriate padding.
     */
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Add top AND bottom padding to avoid system bars
            view.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom
            )

            insets
        }
    }

    /**
     * Sets up button click listeners for the main activity
     */
    private fun setupButtons() {
        // Configure Widgets Button - Opens configuration activity
        binding.btnConfigureWidgets.text = getString(R.string.configure_widgets)
        binding.btnConfigureWidgets.setOnClickListener {
            openConfigurationActivity()
        }

        // Open Battery Settings Button - Opens system battery settings
        binding.btnBatterySettings.text = getString(R.string.battery_settings)
        binding.btnBatterySettings.setOnClickListener {
            openBatterySettings()
        }
    }

    // =========================================================================
    // App Information Methods
    // =========================================================================

    /**
     * Creates comprehensive app information text
     *
     * @return Formatted string containing app version, device info,
     *         and usage instructions
     */
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

        return getFormattedAppInfoText(
            versionName = versionName,
            versionCode = versionCode.toString(),
            minSdkName = getAndroidVersionName(minSdk),
            minSdkCode = minSdk.toString(),
            targetSdkName = getAndroidVersionName(targetSdk),
            targetSdkCode = targetSdk.toString(),
            compileSdkName = getAndroidVersionName(compileSdk),
            compileSdkCode = compileSdk.toString(),
            buildDate = buildDate,
            currentSdkName = getAndroidVersionName(Build.VERSION.SDK_INT),
            currentSdkCode = Build.VERSION.SDK_INT.toString()
        )
    }

    /**
     * Formats app information into a readable string
     *
     * @param versionName App version name
     * @param versionCode App version code
     * @param minSdkName Minimum supported Android version name
     * @param minSdkCode Minimum supported Android version code
     * @param targetSdkName Target Android version name
     * @param targetSdkCode Target Android version code
     * @param compileSdkName Compile SDK version name
     * @param compileSdkCode Compile SDK version code
     * @param buildDate Build date and time
     * @param currentSdkName Current device Android version name
     * @param currentSdkCode Current device Android version code
     * @return Formatted multi-line string with all app information
     */
    private fun getFormattedAppInfoText(
        versionName: String?,
        versionCode: String,
        minSdkName: String,
        minSdkCode: String,
        targetSdkName: String,
        targetSdkCode: String,
        compileSdkName: String,
        compileSdkCode: String,
        buildDate: String,
        currentSdkName: String,
        currentSdkCode: String
    ): String {
        return """
        |${getString(R.string.app_title)}
        |
        |${getString(R.string.app_welcome)}
        |
        |${getString(R.string.features_title)}
        |
        |${getString(R.string.real_time_monitoring)}
        |${getString(R.string.monitoring_bullet_points)}
        |
        |${getString(R.string.visual_design)}
        |${getString(R.string.design_bullet_points)}
        |
        |${getString(R.string.smart_functionality)}
        |${getString(R.string.functionality_bullet_points)}
        |
        |${getString(R.string.configuration_options)}
        |${getString(R.string.configuration_bullet_points)}
        |
        |${getString(R.string.installation_usage)}
        |
        |${getString(R.string.adding_widget)}
        |${getString(R.string.widget_steps)}
        |
        |${getString(R.string.configuring_widget)}
        |${getString(R.string.configuration_tips)}
        |
        |${getString(R.string.battery_settings_access)}
        |${getString(R.string.settings_options)}
        |
        |${getString(R.string.privacy_permissions)}
        |${getString(R.string.privacy_points)}
        |
        |${getString(R.string.app_information)}
        |
        |${getString(R.string.version_info)}
        |${getString(R.string.app_version, versionName, versionCode)}
        |${getString(R.string.min_android, minSdkName, minSdkCode)}
        |${getString(R.string.target_android, targetSdkName, targetSdkCode)}
        |${getString(R.string.compile_sdk, compileSdkName, compileSdkCode)}
        |${getString(R.string.last_updated, buildDate)}
        |
        |${getString(R.string.device_info)}
        |${getString(R.string.current_android, currentSdkName, currentSdkCode)}
        |${getString(R.string.device_model, Build.MANUFACTURER, Build.MODEL)}
        |
        |${getString(R.string.troubleshooting)}
        |
        |${getString(R.string.common_issues)}
        |${getString(R.string.issues_list)}
        |
        |${getString(R.string.need_help)}
        |
        |${getString(R.string.thank_you)}
    """.trimMargin()
    }

    /**
     * Gets the build date from BuildConfig
     *
     * @return Formatted build date string, current date if build time not available
     */
    private fun getBuildDate(): String {
        return try {
            val buildTime = BuildConfig.BUILD_TIME.toLong()
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm", Locale.getDefault())
            dateFormat.format(Date(buildTime))
        } catch (_: Exception) {
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            dateFormat.format(Date())
        }
    }

    /**
     * Gets the minimum SDK version from BuildConfig
     *
     * @return Minimum SDK version, fallback to LOLLIPOP if not available
     */
    private fun getMinSdkVersion(): Int {
        return try {
            BuildConfig.MIN_SDK_VERSION
        } catch (_: Exception) {
            // Fallback to Android 5.0 (Lollipop)
            Build.VERSION_CODES.LOLLIPOP
        }
    }

    /**
     * Gets the target SDK version from BuildConfig
     *
     * @return Target SDK version, fallback to current SDK if not available
     */
    private fun getTargetSdkVersion(): Int {
        return try {
            BuildConfig.TARGET_SDK_VERSION
        } catch (_: Exception) {
            Build.VERSION.SDK_INT
        }
    }

    /**
     * Gets the compile SDK version from BuildConfig
     *
     * @return Compile SDK version, fallback to 34 if not available
     */
    private fun getCompileSdkVersion(): Int {
        return try {
            BuildConfig.COMPILE_SDK_VERSION
        } catch (_: Exception) {
            34 // Fallback to your actual compileSdk
        }
    }

    /**
     * Converts Android API level to version name
     *
     * @param apiLevel Android API level to convert
     * @return Human-readable Android version name
     */
    private fun getAndroidVersionName(apiLevel: Int): String {
        return when (apiLevel) {
            Build.VERSION_CODES.BASE -> getString(R.string.android_1_0)
            Build.VERSION_CODES.BASE_1_1 -> getString(R.string.android_1_1)
            Build.VERSION_CODES.CUPCAKE -> getString(R.string.android_1_5_cupcake)
            Build.VERSION_CODES.DONUT -> getString(R.string.android_1_6_donut)
            Build.VERSION_CODES.ECLAIR -> getString(R.string.android_2_0_eclair)
            Build.VERSION_CODES.ECLAIR_0_1 -> getString(R.string.android_2_0_1_eclair)
            Build.VERSION_CODES.ECLAIR_MR1 -> getString(R.string.android_2_1_eclair)
            Build.VERSION_CODES.FROYO -> getString(R.string.android_2_2_froyo)
            Build.VERSION_CODES.GINGERBREAD -> getString(R.string.android_2_3_gingerbread)
            Build.VERSION_CODES.GINGERBREAD_MR1 -> getString(R.string.android_2_3_3_gingerbread)
            Build.VERSION_CODES.HONEYCOMB -> getString(R.string.android_3_0_honeycomb)
            Build.VERSION_CODES.HONEYCOMB_MR1 -> getString(R.string.android_3_1_honeycomb)
            Build.VERSION_CODES.HONEYCOMB_MR2 -> getString(R.string.android_3_2_honeycomb)
            Build.VERSION_CODES.ICE_CREAM_SANDWICH -> getString(R.string.android_4_0_ice_cream_sandwich)
            Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1 -> getString(R.string.android_4_0_3_ice_cream_sandwich)
            Build.VERSION_CODES.JELLY_BEAN -> getString(R.string.android_4_1_jelly_bean)
            Build.VERSION_CODES.JELLY_BEAN_MR1 -> getString(R.string.android_4_2_jelly_bean)
            Build.VERSION_CODES.JELLY_BEAN_MR2 -> getString(R.string.android_4_3_jelly_bean)
            Build.VERSION_CODES.KITKAT -> getString(R.string.android_4_4_kitkat)
            Build.VERSION_CODES.KITKAT_WATCH -> getString(R.string.android_4_4w_kitkat_wear)
            Build.VERSION_CODES.LOLLIPOP -> getString(R.string.android_5_0_lollipop)
            Build.VERSION_CODES.LOLLIPOP_MR1 -> getString(R.string.android_5_1_lollipop)
            Build.VERSION_CODES.M -> getString(R.string.android_6_0_marshmallow)
            Build.VERSION_CODES.N -> getString(R.string.android_7_0_nougat)
            Build.VERSION_CODES.N_MR1 -> getString(R.string.android_7_1_nougat)
            Build.VERSION_CODES.O -> getString(R.string.android_8_0_oreo)
            Build.VERSION_CODES.O_MR1 -> getString(R.string.android_8_1_oreo)
            Build.VERSION_CODES.P -> getString(R.string.android_9_0_pie)
            Build.VERSION_CODES.Q -> getString(R.string.android_10)
            Build.VERSION_CODES.R -> getString(R.string.android_11)
            Build.VERSION_CODES.S -> getString(R.string.android_12)
            Build.VERSION_CODES.S_V2 -> getString(R.string.android_12l)
            Build.VERSION_CODES.TIRAMISU -> getString(R.string.android_13)
            Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> getString(R.string.android_14)
            Build.VERSION_CODES.VANILLA_ICE_CREAM -> getString(R.string.android_15)
            Build.VERSION_CODES.BAKLAVA -> getString(R.string.android_16)
            else -> {
                if (apiLevel > Build.VERSION_CODES.BAKLAVA) {
                    getString(R.string.android_future_version, apiLevel.toString())
                } else {
                    getString(R.string.android_api_level, apiLevel.toString())
                }
            }
        }
    }

    // =========================================================================
    // Navigation Methods
    // =========================================================================

    /**
     * Opens the widget configuration activity
     *
     * Launches the configuration activity to allow users to configure
     * default settings for new widgets.
     */
    private fun openConfigurationActivity() {
        Log.d("MainActivity", "openConfigurationActivity")
        // Open in informational mode to configure default settings
        val intent = BatteryWidgetConfigureActivity.createConfigurationIntent(this)
        startActivity(intent)
    }

    /**
     * Opens the system battery settings screen
     *
     * Provides access to system-level battery controls and information
     * with multiple fallback options for different Android versions.
     */
    private fun openBatterySettings() {
        Log.d("MainActivity", "openBatterySettings")
        try {
            // Primary option: Battery saver settings (Android 5.0+)
            val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            startActivity(intent)
        } catch (_: Exception) {
            try {
                // Fallback 1: Power usage settings
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
            } catch (_: Exception) {
                try {
                    // Fallback 2: General settings
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    startActivity(intent)
                } catch (_: Exception) {
                    // Final fallback: Open app info
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = "package:$packageName".toUri()
                    startActivity(intent)
                }
            }
        }
    }

    // =========================================================================
    // Unused Methods (Commented for future reference)
    // =========================================================================

    /*
    /**
     * Example of an unused method that could be implemented later
     * for additional functionality like analytics tracking.
     */
    private fun trackUserInteraction() {
        // Implementation for analytics tracking would go here
    }
    */
}