// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    // We only need to declare the main Application plugin here.
    // AGP 9.1.0 will handle the Kotlin/Compose parts automatically.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

allprojects {
    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:deprecation")
    }
}

