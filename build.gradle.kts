// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    id("com.android.application") version "8.8.0" apply false
    id("com.github.spotbugs") version "6.1.4" apply false
    id("org.jetbrains.kotlin.android") version "2.1.10" apply false
}

allprojects {
    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:deprecation")
    }
}
