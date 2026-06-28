import java.net.HttpURLConnection
import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.dirtfy.srr"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dirtfy.srr"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Routes to Firebase Local Emulator Suite — used by unit tests and development
            buildConfigField("Boolean", "USE_EMULATOR", "true")
        }
        release {
            buildConfigField("Boolean", "USE_EMULATOR", "false")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use debug keystore so release APKs can be installed on a dev device.
            // Replace with a real release keystore before distributing to users.
            signingConfig = signingConfigs.getByName("debug")
        }
        // staging: production Firebase + debug signing. Use `./gradlew installStaging` to
        // install an APK that connects to real Firestore/Auth for manual end-to-end testing.
        create("staging") {
            initWith(getByName("debug"))
            buildConfigField("Boolean", "USE_EMULATOR", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

tasks.register("testClasses") {
    dependsOn("compileDebugUnitTestSources")
}

// Clears Firebase Local Emulator Auth + Firestore data from the host before
// instrumented tests run. Prevents account-collision failures when ADB reverse
// drops between runs and the device-side teardown can't reach the emulator.
tasks.register("clearFirebaseEmulator") {
    doLast {
        val projectId = "shared-relative-rank"
        listOf(
            "http://localhost:9099/emulator/v1/projects/$projectId/accounts",
            "http://localhost:8080/emulator/v1/projects/$projectId/databases/(default)/documents"
        ).forEach { url ->
            try {
                val conn = URI(url).toURL().openConnection() as HttpURLConnection
                conn.requestMethod = "DELETE"
                conn.connectTimeout = 3000
                conn.connect()
                val code = conn.responseCode
                conn.disconnect()
                println("Cleared $url — HTTP $code")
            } catch (e: Exception) {
                println("Emulator not reachable at $url — skipping (${e.message})")
            }
        }
    }
}

tasks.whenTaskAdded {
    if (name == "connectedDebugAndroidTest") {
        dependsOn("clearFirebaseEmulator")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // Standard Fragment support
    implementation(libs.androidx.fragment.ktx)
    // Lifecycle support for collectAsStateWithLifecycle
    implementation(libs.androidx.lifecycle.runtime.compose)
    // ViewModels for Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)

    // Drag-to-reorder
    implementation(libs.reorderable)

    // Image loading from URLs
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
