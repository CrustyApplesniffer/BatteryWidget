# Building the Android Application

## Prerequisites

### System Requirements
- **Java Development Kit (JDK) 17** or higher
- **Android SDK** with API level 34
- **Gradle** 8.0+ (included in the project)

### Development Environment
- **Android Studio** (latest version recommended)
- **Git** for version control

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/          # Kotlin/Java source code
│   │   ├── res/           # Resources (layouts, strings, drawables)
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts   # Module-level build configuration
├── build.gradle.kts        # Project-level build configuration
└── gradle.properties       # Gradle properties and secrets
```

## Local Development Build

### 1. Clone the Repository
```bash
git clone https://github.com/CrustyApplesniffer/BatteryWidget.git
cd BatteryWidget
```

### 2. Open in Android Studio
- Open Android Studio
- Select "Open" and choose the project directory
- Wait for Gradle sync to complete

### 3. Build the Application
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK  
./gradlew assembleRelease

# Build Android App Bundle (AAB)
./gradlew bundleRelease

# Run tests
./gradlew test
./gradlew connectedAndroidTest
```

### 4. Install and Run
```bash
# Install debug version on connected device
./gradlew installDebug

# Install release version (if signing config is set up)
./gradlew installRelease
```

## Version Management

### Version Code & Name
The app version is managed in `app/build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        versionCode = 15      // Integer - incremented automatically by CI
        versionName = "1.2.3" // String - semantic versioning
    }
}
```

### Automatic Version Increment
- **versionCode**: Automatically incremented by GitHub Actions on each deployment
- **versionName**: Manually updated for feature releases

## Signing Configuration

### Local Development
For local release builds, create `local.properties`:

```properties
storeFile=/path/to/your/keystore.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

**⚠️ Warning**: Never commit `local.properties` to version control!

### Production Signing
Production signing is handled automatically by GitHub Actions using environment secrets.

## Continuous Integration

### GitHub Actions
The project uses GitHub Actions for automated builds and deployments:

#### Workflows:
- **`deploy-internal.yml`**: Builds and deploys to Play Store Internal Testing
  - Trigger: Manual or push to main/develop branches
  - Actions: Increments versionCode, builds AAB, deploys to Play Store

#### Secrets Required for CI:
- `GCP_SERVICE_ACCOUNT`: Google Play Service Account JSON key
- Signing keys (managed via GitHub Secrets)

### Manual CI Trigger
1. Go to **Actions** tab in GitHub repository
2. Select **"Deploy to Internal Testing"** workflow
3. Click **"Run workflow"**
4. Select branch and click **"Run workflow"**

## Environment-Specific Builds

### Build Types
- **Debug**: Development builds with debugging enabled
- **Release**: Production builds with ProGuard/R8 optimization

### Product Flavors (if applicable)
```bash
# Build specific flavor
./gradlew assembleProductionRelease
./gradlew assembleStagingDebug
```

## Troubleshooting

### Common Issues

#### Gradle Sync Failures
```bash
# Clean and refresh
./gradlew clean
# Delete .gradle directory and re-sync in Android Studio
```

#### Build Failures
```bash
# Check build details
./gradlew build --stacktrace
./gradlew build --info
```

#### Dependency Issues
```bash
# Refresh dependencies
./gradlew --refresh-dependencies
```

### Cache Clearing
```bash
# Clear Gradle cache
./gradlew cleanBuildCache

# Clear Android Studio cache
File → Invalidate Caches / Restart
```

## Code Quality Tools

### Static Analysis
```bash
# Run lint checks
./gradlew lint

# Run ktlint (if configured)
./gradlew ktlintCheck

# Run detekt (if configured)  
./gradlew detekt
```

### Testing
```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run specific test class
./gradlew testDebugUnitTest --tests "com.example.MyTestClass"
```

## Release Process

### 1. Prepare Release
- Update `versionName` in `app/build.gradle.kts`
- Update `CHANGELOG.md`
- Create release branch: `git checkout -b release/v1.2.3`

### 2. Local Verification
```bash
# Build release bundle locally
./gradlew bundleRelease

# Run all tests
./gradlew test connectedAndroidTest lint
```

### 3. Create Pull Request
- Merge release branch to master via PR
- Code review and approval

### 4. Deploy
- GitHub Action will automatically:
  - Increment versionCode
  - Build signed AAB
  - Upload to Google Play Internal Testing
  - Commit versionCode increment

### 5. Promote to Production
- In Google Play Console, promote from Internal Testing to Production

## Dependencies Management

### Adding New Dependencies
1. Add dependency to `app/build.gradle.kts`:
```kotlin
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    testImplementation("junit:junit:4.13.2")
}
```

2. Sync project in Android Studio

### Updating Dependencies
```bash
# Check for dependency updates
./gradlew dependencyUpdates
```

## Performance Tips

### Faster Builds
```bash
# Enable build cache
./gradlew build --build-cache

# Enable parallel execution
./gradlew build --parallel

# Configure Gradle properties in gradle.properties:
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true
```

## Useful Gradle Tasks

| Task | Purpose |
|------|---------|
| `assembleDebug` | Build debug APK |
| `assembleRelease` | Build release APK |
| `bundleRelease` | Build release AAB |
| `installDebug` | Install debug build to device |
| `clean` | Clean build artifacts |
| `lint` | Run Android lint checks |
| `test` | Run unit tests |

## Support

If you encounter build issues:

1. Check this document first
2. Verify all prerequisites are installed
3. Check GitHub Actions logs for CI issues
4. Contact the development team

---

**Last Updated**: 2025-10-20  

**Maintainer**: Crusty Applesniffer
