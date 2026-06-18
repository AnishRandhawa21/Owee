import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)

    kotlin("plugin.serialization")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val supabaseUrl = localProperties.getProperty("SUPABASE_URL")
    ?: project.findProperty("SUPABASE_URL") as? String
    ?: ""

val supabaseKey = localProperties.getProperty("SUPABASE_KEY")
    ?: project.findProperty("SUPABASE_KEY") as? String
    ?: ""

val googleWebClientId = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")
    ?: project.findProperty("GOOGLE_WEB_CLIENT_ID") as? String
    ?: ""

android {
    namespace = "com.owee.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.owee.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"$supabaseUrl\""
        )

        buildConfigField(
            "String",
            "SUPABASE_KEY",
            "\"$supabaseKey\""
        )

        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"$googleWebClientId\""
        )
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.ui.graphics)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.4")

// ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.3")

// Material Icons
    implementation("androidx.compose.material:material-icons-extended")

// Coil (Image Loading)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

// Supabase
    implementation("io.github.jan-tennert.supabase:compose-auth:3.2.5")
    implementation("io.github.jan-tennert.supabase:auth-kt:3.2.5")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.2.5")

// Kotlin Serialization (Required by Supabase)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

// Ktor
    implementation("io.ktor:ktor-client-okhttp:3.2.3")

// Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("io.github.jan-tennert.supabase:realtime-kt:3.2.5")

    implementation("androidx.compose.foundation:foundation:1.6.0")
}
