plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.compose)
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "me.hackerchick.catima.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "me.hackerchick.catima"
        minSdk = 26
        targetSdk = 36
        versionCode = 200
        versionName = "1.0.0 Wear OS"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        compose = true
    }

    lint {
        lintConfig = file("lint.xml")
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.core.core.ktx)
    implementation(libs.androidx.activity.activity.compose)

    // AndroidX
    implementation(libs.androidx.core.core.splashscreen)

    // Wear OS Compose
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.navigation)

    // ZXing for barcode rendering
    implementation(libs.com.google.zxing.core)
}
