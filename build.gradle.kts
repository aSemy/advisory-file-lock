@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17

plugins {
  alias(libs.plugins.kotlin.jvm)
  idea
}

group = "dev.adamko.advisoryfilelock"
version = "0.0.1"

testing {
  suites.withType<JvmTestSuite>().configureEach {
    useJUnitJupiter()
  }
  val test by suites.getting(JvmTestSuite::class) {
    dependencies {
      implementation("org.jetbrains.kotlin:kotlin-test")
    }
  }
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

tasks.updateDaemonJvm {
  languageVersion = JavaLanguageVersion.of(21)
}

idea {
  module {
    excludeDirs.addAll(
      listOf(
        ".kotlin",
        ".idea",
        "gradle/wrapper",
      ).map(::file)
    )
  }
}
