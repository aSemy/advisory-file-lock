plugins {
  alias(libs.plugins.kotlin.jvm)
  application
}

repositories {
  mavenCentral()
}

dependencies {
}

testing {
  suites {
    val test by getting(JvmTestSuite::class) {
    }
  }
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(17)
  }
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
