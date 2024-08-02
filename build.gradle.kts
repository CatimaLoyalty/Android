// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    id("com.android.application") version "8.5.1" apply false
    id("com.github.spotbugs") version "5.1.4" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
}

allprojects {
    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:deprecation")
    }
}
