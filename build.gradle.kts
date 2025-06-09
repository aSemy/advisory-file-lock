@file:Suppress("UnstableApiUsage")
@file:OptIn(ExperimentalSerializationApi::class)

import java.net.URI
import kotlin.io.path.toPath
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import model.GradleModuleMetadata
import model.MutableGradleModuleMetadata
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17

plugins {
  alias(libs.plugins.kotlin.jvm)
  `maven-publish`
  idea
}

group = "aSemy.advisory-file-lock"
version = object {
  private val version: Provider<String> = project.gitVersion
  override fun toString(): String = version.orNull ?: "unknown"
}

testing {
  suites.withType<JvmTestSuite>().configureEach {
    useJUnitJupiter()
  }
  val test by suites.getting(JvmTestSuite::class) {
    dependencies {
      implementation(libs.junit.jupiter)
      runtimeOnly(libs.junit.platformLauncher)

      implementation(platform(libs.kotlinx.coroutines.bom))
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.kotlinx.coroutines.test)

      implementation(libs.kotest.assertions)
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

java {
  withSourcesJar()
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
        "buildSrc/.kotlin",
        ".idea",
        "gradle/wrapper",
      ).map(::file)
    )
  }
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["java"])
    }
  }
}

val buildDirMavenRepo =
  publishing.repositories.maven(layout.buildDirectory.dir("build-dir-maven")) {
    name = "BuildDir"
  }

fun buildDirMavenDirectoryProvider(): Provider<Directory> =
  objects.directoryProperty().fileProvider(provider { buildDirMavenRepo }.map { it.url.toPath().toFile() })

val buildDirPublishTasks = tasks.withType<PublishToMavenRepository>().matching { task ->
  task.repository?.name == buildDirMavenRepo.name
}

val cleanBuildDirDirectory by tasks.registering(Delete::class) {
  val buildDirMavenRepoDir = buildDirMavenDirectoryProvider()
  delete(buildDirMavenRepoDir)
}

buildDirPublishTasks.configureEach {
  val repositoryUrl: Provider<URI> = provider { repository.url }

  outputs.dir(repositoryUrl)
    .withPropertyName("repositoryUrl")

  dependsOn(cleanBuildDirDirectory)
}

tasks.withType<PublishToMavenRepository>()
  .matching { it.repository.name == "BuildDir" }

val prepareGitHubReleaseFiles by tasks.registering {
  group = "publishing"

  val fs = serviceOf<FileSystemOperations>()

  dependsOn(tasks.named("publishAllPublicationsToBuildDirRepository"))

  val buildDirMavenRepoDir = buildDirMavenDirectoryProvider()
  inputs.files(buildDirMavenRepoDir)
    .withPropertyName("buildDirMavenRepoDir")
    .withPathSensitivity(RELATIVE)

  val destinationDir = layout.buildDirectory.dir("github-release-files")
  outputs.dir(destinationDir)

  doLast {
    val repoDir = buildDirMavenRepoDir.get().asFile
    println("[$path] processing buildDirMavenRepo: $repoDir")

    val json = Json {
      prettyPrint = true
      prettyPrintIndent = "  "
    }

    fs.sync {
      into(destinationDir)
      include(
        "**/*.jar",
        "**/*.module",
      )
      exclude(
        "**/*.md5",
        "**/*.sha1",
        "**/*.sha256",
        "**/*.sha512",
        "**/maven-metadata.xml",
        "**/*.pom",
      )
      includeEmptyDirs = false

      val moduleMetadataFiles = repoDir.walk()
        .filter { it.isFile && it.extension == "module" }
        .filter { moduleFile ->
          try {
            moduleFile.inputStream().use { source ->
              json.decodeFromStream(MutableGradleModuleMetadata.serializer(), source)
            }
            true
          } catch (ex: IllegalArgumentException) {
            logger.lifecycle("[$path] ${moduleFile.name} is not a valid Gradle module metadata file: $ex")
            false
          } catch (ex: SerializationException) {
            logger.lifecycle("[$path] ${moduleFile.name} is not a valid Gradle module metadata file: $ex ")
            false
          }
        }

      //region update gradle module metadata
      moduleMetadataFiles.forEach { moduleFile ->
        val moduleMetadata = moduleFile.inputStream().use { source ->
          json.decodeFromStream(MutableGradleModuleMetadata.serializer(), source)
        }
        if (moduleMetadata.component.url?.startsWith("../../") == true) {
          moduleMetadata.component.url = moduleMetadata.component.url?.substringAfterLast("/")
        }

        moduleMetadata.variants.forEach { variant ->
          variant.availableAt?.let { aa ->
            if (aa.url.startsWith("../../")) {
              aa.url = aa.url.substringAfterLast("/")
            }
          }
        }
        moduleFile.outputStream().use { sink ->
          json.encodeToStream(MutableGradleModuleMetadata.serializer(), moduleMetadata, sink)
        }
      }
      //endregion

      moduleMetadataFiles
        .forEach { moduleFile ->
          val moduleMetadata = moduleFile.inputStream().use { stream ->
            Json.decodeFromStream(GradleModuleMetadata.serializer(), stream)
          }
          val moduleVersion = moduleMetadata.component.version
          val moduleName = moduleMetadata.component.module

          from(moduleFile.parentFile)

          val snapshotVersion = moduleFile.nameWithoutExtension
            .substringAfter("$moduleName-", "")

          val isSnapshot = moduleVersion.endsWith("-SNAPSHOT") && snapshotVersion != moduleVersion

          eachFile {
            // Update filenames:
            // - Rename the snapshot timestamp with 'SNAPSHOT'.
            // - Remove directories (can't attach directories to GitHub Release).

            val newFileName = if (isSnapshot) {
              sourceName.replace("-$snapshotVersion", "-${moduleVersion}")
            } else {
              sourceName
            }

            relativePath = RelativePath(true, newFileName)
          }
        }
    }

    destinationDir.get().asFile.walk()
      .filter { it.isFile && it.name.endsWith(".module") }
      .forEach { moduleFile ->

        setOf(
          "256",
          "512",
        ).forEach {
          val checksum = moduleFile.computeChecksum("SHA-$it")
          moduleFile.resolveSibling(moduleFile.name + ".sha$it").writeText(checksum)
        }

        val metadata = moduleFile.inputStream().use { stream ->
          Json.decodeFromStream(GradleModuleMetadata.serializer(), stream)
        }
        val rootModuleName = metadata.component.module + "-" + metadata.component.version
        if (moduleFile.nameWithoutExtension == rootModuleName) {
          moduleFile.resolveSibling("advisory-file-lock.ivy.xml")
            .writeText( // language=xml
              """
              |<?xml version="1.0"?>
              |<ivy-module version="2.0"
              |            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              |            xsi:noNamespaceSchemaLocation="https://ant.apache.org/ivy/schemas/ivy.xsd">
              |    <!-- do_not_remove: published-with-gradle-metadata -->
              |    <info organisation="${metadata.component.group}" module="${metadata.component.module}" revision="${metadata.component.version}" />
              |</ivy-module>
              |""".trimMargin()
            )
        }

        destinationDir.get().asFile

        logger.lifecycle("[$path] outputDir:${destinationDir.get().asFile.invariantSeparatorsPath}")
      }
  }
}
