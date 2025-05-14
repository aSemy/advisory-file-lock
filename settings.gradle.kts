@file:Suppress("UnstableApiUsage")

rootProject.name = "lokka"

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.PREFER_SETTINGS
  repositories {
    mavenCentral()
  }
}

include(":demo")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
