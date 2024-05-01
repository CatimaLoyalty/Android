// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    id("com.android.application") version "8.4.0" apply false
    id("com.github.spotbugs") version "6.0.13" apply false
}

allprojects {
    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:deprecation")
    }
}
