import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Read optional settings from local.properties (git-ignored). The CWA key
// falls back to an environment variable (useful for CI) and is empty when
// neither is set. Only debug builds ever embed the key; release builds never do.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

val cwaApiKey: String = (localProps.getProperty("CWA_API_KEY")
    ?: System.getenv("CWA_API_KEY")
    ?: "")
    .trim()

// Escape so a value is always a valid Java string literal in BuildConfig.
fun javaLiteral(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val cwaApiKeyLiteral: String = javaLiteral(cwaApiKey)

// Base URL of the proxy that holds the CWA key server-side.
val proxyBaseUrl: String = (localProps.getProperty("PROXY_BASE_URL")
    ?: "https://fairweather-proxy.vercel.app")
    .trim()
val proxyBaseUrlLiteral: String = javaLiteral(proxyBaseUrl)

android {
    namespace = "com.artchknd.tryit"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.artchknd.tryit"
        minSdk = 31
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "PROXY_BASE_URL", proxyBaseUrlLiteral)
    }

    buildTypes {
        debug {
            // Debug builds may call CWA directly when a key is configured.
            buildConfigField("String", "CWA_API_KEY", cwaApiKeyLiteral)
        }
        release {
            // Release builds never embed a key; they go through the proxy.
            buildConfigField("String", "CWA_API_KEY", "\"\"")
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
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("io.github.kyant0:backdrop:2.0.0")
}