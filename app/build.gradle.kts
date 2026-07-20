import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Release signing is read from a git-ignored keystore.properties (see
// keystore.properties.template). Absent it, release builds stay unsigned.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.gymgym.app"
    compileSdk = 35

    defaultConfig {
        // Store/package name (Play). Code namespace stays com.gymgym.app.
        applicationId = "com.projectorum.gymgym"
        minSdk = 26
        targetSdk = 35
        versionCode = 15
        versionName = "2.0.6"

        // --- Ad tuning knobs (see AGENTS.md) ---
        // Minimum time between interstitials, gated at "open a workout". 5 min.
        buildConfigField("long", "AD_INTERVAL_MS", "300000L")
        // Real AdMob interstitial ad-unit id.
        buildConfigField(
            "String",
            "AD_INTERSTITIAL_UNIT_ID",
            "\"ca-app-pub-3275806583625659/5269185261\"",
        )
    }

    flavorDimensions += "tier"
    productFlavors {
        create("free") {
            dimension = "tier"
            buildConfigField("boolean", "ADS_ENABLED", "true")
        }
        create("paid") {
            dimension = "tier"
            applicationIdSuffix = ".paid"
            versionNameSuffix = "-paid"
            buildConfigField("boolean", "ADS_ENABLED", "false")
        }
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            // Let Robolectric resolve real string resources so formatting/label
            // helpers (which now localize via Context.getString) are testable.
            isIncludeAndroidResources = true
        }
    }

    lint {
        // Exercise-library names ship English-first and rely on Android's
        // automatic resource fallback; localizing the full ~30-name catalog is a
        // tracked follow-up. Chrome/UI strings stay fully translated by convention.
        disable += "MissingTranslation"
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val cameraxVersion = "1.4.0"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-video:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    implementation("com.google.mlkit:pose-detection:18.0.0-beta5")

    // Ads — free flavor only, so the paid build never links the SDK or network.
    "freeImplementation"("com.google.android.gms:play-services-ads:23.6.0")
    "freeImplementation"("com.google.android.ump:user-messaging-platform:3.1.0")
    // play-services-ads swaps in an empty ListenableFuture stub that shadows the
    // one CameraX needs; full Guava restores the real class on the free classpath.
    "freeImplementation"("com.google.guava:guava:33.3.1-android")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core-ktx:1.6.1")
}
