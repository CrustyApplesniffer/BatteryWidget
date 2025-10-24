plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Kotlin-only approach to read local.properties
fun getLocalProperty(key: String): String? {
    val localPropertiesFile = rootProject.file("local.properties")
    if (!localPropertiesFile.exists()) return null

    return try {
        localPropertiesFile.useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .find { it.startsWith("$key=") }
                ?.substringAfter("=")
                ?.trim()
        }
    } catch (e: Exception) {
        null
    }
}

android {
    namespace = "com.prometeo.batterywidget"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.prometeo.batterywidget"
        minSdk = 23
        targetSdk = 35
        versionCode = 4
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Add build time and SDK info to BuildConfig
        buildConfigField("String", "BUILD_TIME", "\"${System.currentTimeMillis()}\"")
        buildConfigField("int", "MIN_SDK_VERSION", "$minSdk")
        buildConfigField("int", "TARGET_SDK_VERSION", "$targetSdk")
        buildConfigField("int", "COMPILE_SDK_VERSION", "$compileSdk")
    }

    signingConfigs {
        create("release") {
            // storeFile:  keystore local file OR provided by CI OR debug.keystore
            val keystorePath = getLocalProperty("keystore.file")
                ?: System.getenv("KEYSTORE_FILE")
                ?: "debug.keystore"

            storeFile = file(keystorePath)

            storePassword =
                getLocalProperty("keystore.password") ?:
                        System.getenv("KEYSTORE_PASSWORD") ?:
                        "android"
            keyAlias =
                getLocalProperty("key.alias") ?:
                        System.getenv("KEY_ALIAS") ?:
                        "androiddebugkey"
            keyPassword =
                getLocalProperty("key.password") ?:
                        System.getenv("KEY_PASSWORD") ?:
                        "android"

            // Optional: useful in CI to avoid crashing if the keystore is missing
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    
    // For coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
