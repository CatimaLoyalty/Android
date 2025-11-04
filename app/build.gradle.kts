import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "protect.card_locker"
    compileSdk = 36

    defaultConfig {
        applicationId = "me.hackerchick.catima"
        minSdk = 21
        targetSdk = 36
        versionCode = 155
        versionName = "2.39.2"

        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true

        resourceConfigurations += listOf("ar", "be", "bg", "bn", "bn-rIN", "bs", "cs", "da", "de", "el-rGR", "en", "eo", "es", "es-rAR", "et", "fa", "fi", "fr", "gl", "he-rIL", "hi", "hr", "hu", "in-rID", "is", "it", "ja", "ko", "lt", "lv", "nb-rNO", "nl", "oc", "pl", "pt", "pt-rBR", "pt-rPT", "ro-rRO", "ru", "sk", "sl", "sr", "sv", "ta", "tr", "uk", "vi", "zh-rCN", "zh-rTW")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("boolean", "showDonate", "true")
        buildConfigField("boolean", "showRateOnGooglePlay", "false")
        buildConfigField("boolean", "useAcraCrashReporter", "true")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    flavorDimensions.add("type")
    productFlavors {
        create("foss") {
            dimension = "type"
            isDefault = true
        }
        create("gplay") {
            dimension = "type"

            // Google doesn't allow donation links
            buildConfigField("boolean", "showDonate", "false")
            buildConfigField("boolean", "showRateOnGooglePlay", "true")

            // Google Play already sends crashes to the Google Play Console
            buildConfigField("boolean", "useAcraCrashReporter", "false")
        }
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    compileOptions {
        encoding = "UTF-8"

        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("test") {
            resources.srcDirs("src/test/res")
        }
    }

    // Starting with Android Studio 3 Robolectric is unable to find resources.
    // The following allows it to find the resources.
    testOptions.unitTests.isIncludeAndroidResources = true
    tasks.withType<Test>().configureEach {
        testLogging {
            events("started", "passed", "skipped", "failed")
        }
    }

    lint {
        lintConfig = file("lint.xml")
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    // AndroidX
    implementation(libs.androidx.appcompat.appcompat)
    implementation(libs.androidx.constraintlayout.constraintlayout)
    implementation(libs.androidx.core.core.ktx)
    implementation(libs.androidx.core.core.remoteviews)
    implementation(libs.androidx.core.core.splashscreen)
    implementation(libs.androidx.exifinterface.exifinterface)
    implementation(libs.androidx.palette.palette)
    implementation(libs.androidx.preference.preference)
    implementation(libs.com.google.android.material.material)
    coreLibraryDesugaring(libs.com.android.tools.desugar.jdk.libs)

    // Third-party
    implementation(libs.com.journeyapps.zxing.android.embedded)
    implementation(libs.com.github.yalantis.ucrop)
    implementation(libs.com.google.zxing.core)
    implementation(libs.org.apache.commons.commons.csv)
    implementation(libs.com.jaredrummler.colorpicker)
    implementation(libs.net.lingala.zip4j.zip4j)

    // Crash reporting
    implementation(libs.bundles.acra)

    // Testing
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.junit.junit)
    testImplementation(libs.org.robolectric.robolectric)

    androidTestImplementation(libs.bundles.androidx.test)
    androidTestImplementation(libs.junit.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.uiautomator.uiautomator)
    androidTestImplementation(libs.androidx.test.espresso.espresso.core)
}

tasks.register("copyRawResFiles", Copy::class) {
    from(
        layout.projectDirectory.file("../CHANGELOG.md"),
        layout.projectDirectory.file("../PRIVACY.md")
    )
    into(layout.projectDirectory.dir("src/main/res/raw"))
    rename { it.lowercase() }
}.also {
    tasks.preBuild.dependsOn(it)
    tasks.getByName<Delete>("clean") {
        val filesNamesToDelete = listOf("CHANGELOG", "PRIVACY")
        filesNamesToDelete.forEach { fileName ->
            delete(layout.projectDirectory.file("src/main/res/raw/${fileName.lowercase()}.md"))
        }
    }
}
