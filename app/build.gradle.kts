import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Load properties from local.properties
val properties = Properties().apply {
    load(project.rootProject.file("local.properties").inputStream())
}

android {
    namespace = "com.example.smslinkchecker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.smslinkchecker"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Reading the API keys from local.properties
        // Accessing the API keys
        val vtApiKey = properties.getProperty("VT_API_KEY", "")
        buildConfigField("String", "VT_API_KEY", "\"$vtApiKey\"")

        val ssApiKey = properties.getProperty("SS_API_KEY", "")
        buildConfigField("String", "SS_API_KEY", "\"$ssApiKey\"")
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
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation ("com.squareup.okhttp3:okhttp:4.9.2")
}