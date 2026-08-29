// app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace  = "com.visiontv.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.visiontv.app"
        minSdk        = 24
        targetSdk     = 37
        versionCode   = 1
        versionName   = "1.0.0"

        buildConfigField("String", "TMDB_TOKEN", "\"${project.findProperty("TMDB_TOKEN") ?: ""}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = false
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.prev)
    debugImplementation(libs.compose.ui.tooling)

    // Android TV
    implementation(libs.tv.material)
    implementation(libs.tv.foundation)

    // Navigation
    implementation(libs.navigation.compose)

    // Lifecycle / ViewModel
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)

    // Coroutines
    implementation(libs.coroutines.android)

    // Media3 / ExoPlayer
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.hls)
    implementation(libs.media3.ui)
    implementation(libs.media3.okhttp)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)

    // Imágenes
    implementation(libs.coil.compose)
    implementation(libs.coil.okhttp)

    // Persistencia
    implementation(libs.datastore)

    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.compose.material.icons.extended)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.coroutines.test)
}
