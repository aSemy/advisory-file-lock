plugins {
  alias(libs.plugins.kotlin.jvm)
  application
  kotlin("plugin.serialization") version libs.versions.kotlin
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
}

testing {
  suites {
    val test by getting(JvmTestSuite::class) {
    }
  }
}

kotlin {
  jvmToolchain(17)
}

tasks.installDist {
  eachFile {
    if (relativePath.pathString.startsWith("filelock/lib")) {
      permissions {
        other { write = true }
        group { write = true }
        user { write = true }
      }
    }
  }
}

application {
  mainClass = "demo.Main2Kt"
}

tasks.run.configure {
  // lower, because I want to trigger GC and lockRef expiration
  maxHeapSize = "32m"
}
