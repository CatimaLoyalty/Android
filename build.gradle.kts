// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    id("com.android.application") version "8.3.1" apply false
    id("com.github.spotbugs") version "6.0.9" apply false
}

allprojects {
    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:deprecation")
    }
}
