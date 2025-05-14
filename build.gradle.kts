@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17

plugins {
  alias(libs.plugins.kotlin.jvm)
//  kotlin("plugin.serialization") version libs.versions.kotlin
  idea
}

group = "dev.adamko.lokka"
version = "0.0.1"

dependencies {
//  implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
}

testing {
  suites.withType<JvmTestSuite>().configureEach {
    useJUnitJupiter()
  }
  val test by suites.getting(JvmTestSuite::class) {
    dependencies {
      implementation("org.jetbrains.kotlin:kotlin-test")
//      implementation(project.dependencies.kotlin("test"))
    }
  }
//  suites {
//    val test by getting(JvmTestSuite::class) {
//    }
//  }
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

//java {
//  toolchain {
//    languageVersion = javaToolchainVersion
//  }
//}

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
