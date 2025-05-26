plugins {
  `kotlin-dsl`
  kotlin("plugin.serialization") version embeddedKotlinVersion
}

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
}
