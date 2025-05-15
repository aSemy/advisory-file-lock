import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17

plugins {
  alias(libs.plugins.kotlin.jvm)
  application
}

group = "dev.adamko.advisoryfilelock.demo"
version = "0.0.1"

dependencies {
  implementation(project(":"))
//  implementation(projects.filelock)
}

application {
//  mainClass = "dev.adamko.advisoryfilelock.demo.Main1Kt"
  mainClass = "dev.adamko.advisoryfilelock.demo.Main2Kt"
}

tasks.run.configure {
  // lower, because I want to trigger GC and lockRef expiration
  maxHeapSize = "32m"
}

kotlin {
  jvmToolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
  compilerOptions {
    jvmTarget = JVM_17
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.release = kotlin.compilerOptions.jvmTarget.map { it.target.toInt() }
}
