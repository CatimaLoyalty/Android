import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.github.spotbugs.snom.SpotBugsTask

plugins {
    id("com.android.application")
    id("com.github.spotbugs")
    id("org.jetbrains.kotlin.android")
}

spotbugs {
    ignoreFailures.set(false)
    setEffort("max")
    excludeFilter.set(file("./config/spotbugs/exclude.xml"))
    reportsDir.set(layout.buildDirectory.file("reports/spotbugs/").get().asFile)
}

android {
    namespace = "protect.card_locker"
    compileSdk = 34

    defaultConfig {
        applicationId = "me.hackerchick.catima"
        minSdk = 21
        targetSdk = 34
        versionCode = 141
        versionName = "2.33.0"

        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true

        resourceConfigurations += listOf("ar", "bg", "bn", "bn-rIN", "bs", "cs", "da", "de", "el-rGR", "en", "eo", "es", "es-rAR", "et", "fi", "fr", "gl", "he-rIL", "hi", "hr", "hu", "in-rID", "is", "it", "ja", "ko", "lt", "lv", "nb-rNO", "nl", "oc", "pl", "pt-rBR", "pt-rPT", "ro-rRO", "ru", "sk", "sl", "sr", "sv", "tr", "uk", "vi", "zh-rCN", "zh-rTW")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        jvmTarget = "17"
    }
}

dependencies {
    // AndroidX
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.palette:palette:1.0.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("com.google.android.material:material:1.12.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")

    // Third-party
    implementation("com.journeyapps:zxing-android-embedded:4.3.0@aar")
    implementation("com.github.yalantis:ucrop:2.2.10")
    implementation("com.google.zxing:core:3.5.3")
    implementation("org.apache.commons:commons-csv:1.9.0")
    implementation("com.jaredrummler:colorpicker:1.1.0")
    implementation("net.lingala.zip4j:zip4j:2.11.5")

    // SpotBugs
    implementation("io.wcm.tooling.spotbugs:io.wcm.tooling.spotbugs.annotations:1.0.0")

    // Testing
    val androidXTestVersion = "1.6.1"
    val junitVersion = "4.13.2"
    testImplementation("androidx.test:core:$androidXTestVersion")
    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.robolectric:robolectric:4.14.1")

    androidTestImplementation("androidx.test:core:$androidXTestVersion")
    androidTestImplementation("junit:junit:$junitVersion")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:$androidXTestVersion")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

tasks.withType<SpotBugsTask>().configureEach {
    description = "Run spotbugs"
    group = "verification"

    //classes = fileTree("build/intermediates/javac/debug/compileDebugJavaWithJavac/classes")
    //source = fileTree("src/main/java")
    //classpath = files()

    reports.maybeCreate("xml").required.set(false)
    reports.maybeCreate("html").required.set(true)
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
