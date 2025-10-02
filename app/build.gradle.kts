import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "protect.card_locker"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "me.hackerchick.catima"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 152
        versionName = "2.38.0"

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
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.core.ktx)
    implementation(libs.core.remoteviews)
    implementation(libs.core.splashscreen)
    implementation(libs.exifinterface)
    implementation(libs.palette)
    implementation(libs.preference)
    implementation(libs.material)
    coreLibraryDesugaring(libs.desugar)

    // Third-party
    implementation(libs.zxing.embedded)
    implementation(libs.ucrop)
    implementation(libs.zxing.core)
    implementation(libs.commons.csv)
    implementation(libs.colorpicker)
    implementation(libs.zip4j)

    // Crash reporting
    implementation(libs.acra.mail)
    implementation(libs.acra.dialog)

    // Testing
    testImplementation(libs.test.core)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.test.core)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.uiautomator)
    androidTestImplementation(libs.espresso.core)
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
