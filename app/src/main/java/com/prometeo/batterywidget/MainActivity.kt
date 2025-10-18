package com.prometeo.batterywidget

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
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
            currentSdkCode = Build.VERSION.SDK_INT.toString(),
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL
        )
    }

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
        currentSdkCode: String,
        manufacturer: String,
        model: String
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
        |${getString(R.string.device_model, manufacturer, model)}
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

    private fun getMinSdkVersion(): Int {
        return try {
            BuildConfig.MIN_SDK_VERSION
        } catch (_: Exception) {
            // Fallback
            Build.VERSION_CODES.LOLLIPOP
        }
    }

    private fun getTargetSdkVersion(): Int {
        return try {
            BuildConfig.TARGET_SDK_VERSION
        } catch (_: Exception) {
            Build.VERSION.SDK_INT
        }
    }

    private fun getCompileSdkVersion(): Int {
        return try {
            BuildConfig.COMPILE_SDK_VERSION
        } catch (_: Exception) {
            34 // Fallback to your actual compileSdk
        }
    }

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
            else -> {
                if (apiLevel > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    getString(R.string.android_future_version, apiLevel.toString())
                } else {
                    getString(R.string.android_api_level, apiLevel.toString())
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
}