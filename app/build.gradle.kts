import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

val otpProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

fun quoteProperty(value: String?): String {
    return value?.replace("\\", "\\\\")?.replace("\"", "\\\"") ?: ""
}

fun booleanProperty(key: String, default: Boolean): Boolean {
    return otpProperties.getProperty(key)?.toBooleanStrictOrNull() ?: default
}

fun intProperty(key: String, default: Int): Int {
    return otpProperties.getProperty(key)?.toIntOrNull() ?: default
}

android {
    namespace = "com.example.nptudttbdd"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.nptudttbdd"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "OTP_EMAIL_ADDRESS",
            "\"${quoteProperty(otpProperties.getProperty("otp.email.address"))}\""
        )
        buildConfigField(
            "String",
            "OTP_EMAIL_PASSWORD",
            "\"${quoteProperty(otpProperties.getProperty("otp.email.password"))}\""
        )
        buildConfigField(
            "String",
            "OTP_EMAIL_HOST",
            "\"${quoteProperty(otpProperties.getProperty("otp.email.host") ?: "smtp.gmail.com")}\""
        )
        buildConfigField(
            "String",
            "OTP_EMAIL_SENDER_NAME",
            "\"${quoteProperty(otpProperties.getProperty("otp.email.sender_name") ?: "Travelover Support")}\""
        )
        buildConfigField(
            "int",
            "OTP_EMAIL_PORT",
            intProperty("otp.email.port", 587).toString()
        )
        buildConfigField(
            "boolean",
            "OTP_EMAIL_USE_TLS",
            booleanProperty("otp.email.use_tls", true).toString()
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        buildConfig = true
        compose = true
    }
}

dependencies {
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-database:21.0.0")
    implementation("com.google.firebase:firebase-storage:21.0.0")
    implementation("com.google.firebase:firebase-auth:23.1.0")

    implementation("com.google.gms.google-services:com.google.gms.google-services.gradle.plugin:4.4.4")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}