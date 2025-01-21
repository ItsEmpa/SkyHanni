import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    java
    id("com.gradleup.shadow") version "8.3.4"
    kotlin("jvm")
}

sourceSets {
    val dummy by creating
    main {
        dummy.compileClasspath += compileClasspath
        compileClasspath += dummy.output
        output.setResourcesDir(java.classesDirectory)
    }
}

java.toolchain {
    languageVersion = JavaLanguageVersion.of(8)
}

repositories {
    maven("https://jitpack.io")
    maven("https://maven.minecraftforge.net")
}

dependencies {
    compileOnly("com.github.hannibal002:notenoughupdates:4957f0b:all") {
        exclude(module = "unspecified")
        //isTransitive = false
    }
    compileOnly("com.google.code.gson:gson:2.10.1")
}


kotlin {
    jvmToolchain(8)
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
