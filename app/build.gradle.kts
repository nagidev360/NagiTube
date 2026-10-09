import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use(localProperties::load)
}
val supabaseUrl = providers.gradleProperty("SUPABASE_URL")
    .orElse(providers.environmentVariable("SUPABASE_URL"))
    .orElse(localProperties.getProperty("SUPABASE_URL") ?: "")
    .get()
val supabaseAnonKey = providers.gradleProperty("SUPABASE_ANON_KEY")
    .orElse(providers.environmentVariable("SUPABASE_ANON_KEY"))
    .orElse(localProperties.getProperty("SUPABASE_ANON_KEY") ?: "")
    .get()
val youtubeApiKey = providers.gradleProperty("YOUTUBE_API_KEY")
    .orElse(providers.environmentVariable("YOUTUBE_API_KEY"))
    .orElse(localProperties.getProperty("YOUTUBE_API_KEY") ?: "")
    .get()

android {
    namespace = "com.nagidev360.nagitube"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nagidev360.nagitube"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
        buildConfigField("String", "YOUTUBE_API_KEY", "\"$youtubeApiKey\"")
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("io.coil-kt:coil-compose:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
