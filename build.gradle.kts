@file:Suppress("UnstableApiUsage")

import java.net.URI
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.toPath
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

publishing {
  repositories {
    maven(layout.buildDirectory.dir("build-dir-maven")) {
      name = "BuildDir"
    }
  }
  publications {
    create<MavenPublication>("maven") {
      from(components["java"])
    }
  }
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


tasks.withType<PublishToMavenRepository>()
  .matching { task ->
    try {
      task.repository.url.toPath()
      true
    } catch (ex: IllegalArgumentException) {
      false
    }
  }
  .configureEach {
    val repositoryUrl: Provider<URI> = provider { repository.url }

    outputs.dir(repositoryUrl)
      .withPropertyName("repositoryUrl")

    doFirst {
      @OptIn(ExperimentalPathApi::class)
      repositoryUrl.get().toPath().deleteRecursively()
    }
  }

val publishMavenPublicationToBuildDirRepository =
  tasks.named<PublishToMavenRepository>("publishMavenPublicationToBuildDirRepository")

val prepareGitHubReleaseFiles by tasks.registering {
  group = "publishing"

  val fs = serviceOf<FileSystemOperations>()

  val publicationDir: Provider<FileCollection> =
    publishMavenPublicationToBuildDirRepository.map { it.outputs.files }
  inputs.files(publicationDir)
    .withPropertyName("publicationDir")
    .withPathSensitivity(RELATIVE)

  val publicationGroup = publishMavenPublicationToBuildDirRepository.map { it.publication.groupId }
  val publicationArtifactId = publishMavenPublicationToBuildDirRepository.map { it.publication.artifactId }
  val publicationVersion = publishMavenPublicationToBuildDirRepository.map { it.publication.version }
  inputs.property("publicationGroup", publicationGroup)
  inputs.property("publicationArtifactId", publicationArtifactId)
  inputs.property("publicationVersion", publicationVersion)

  outputs.dir(temporaryDir)

  doLast {
    logger.lifecycle("[$path] publication group: ${publicationGroup.get()}")
    logger.lifecycle("[$path] publication artifact: ${publicationArtifactId.get()}")
    logger.lifecycle("[$path] publication version: ${publicationVersion.get()}")

    val repoDir = publicationDir.get().singleFile

    val mavenMetadataContent = repoDir.walk()
      .filter { it.isFile && it.parentFile.name == publicationVersion.orNull }
      .firstOrNull { it.name == "maven-metadata.xml" }
      ?.readText()
      .orEmpty()

    val snapshotVersions = mavenMetadataContent
      .substringAfter("<snapshotVersions>", "")
      .substringBefore("</snapshotVersions>", "")
      .split("<snapshotVersion>")
      .mapNotNull { v ->
        v.substringAfter("<value>", "")
          .substringBefore("</value>", "")
          .ifBlank { null }
      }
      .toSet()

//    val renamedFiles = mutableMapOf<String, String>()

    fs.sync {
      into(temporaryDir)
      from(repoDir)
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
      eachFile {
        // Update filenames:
        // - Rename the snapshot timestamp with 'SNAPSHOT'.
        // - Remove directories (can't attach directories to GitHub Release).

        val snapshotVersion = snapshotVersions.firstOrNull { version ->
          "-$version" in sourceName
        }

        val newFileName =
          if (snapshotVersion != null) {
            sourceName.replace("-$snapshotVersion", "-${publicationVersion.get()}")
          } else {
            sourceName
          }

//        val newFileName = buildList {
//          add(publicationGroup.get())
//          add(filename)
//        }.joinToString("-")

        relativePath = RelativePath(true, newFileName)

//        renamedFiles[newFileName] = newFileName
      }
      includeEmptyDirs = false
    }

//    println(
//      "renamedFiles: ${
//        renamedFiles.entries.sortedBy { it.key }.joinToString(
//          "\n",
//          prefix = "\n"
//        ) { (oldName, newName) -> "  ${oldName.padEnd(70, ' ')} -> $newName" }
//      }"
//    )

    temporaryDir.walk()
      .filter { it.isFile && it.name.endsWith(".module") }
      .forEach { file ->

//        renamedFiles.forEach { (oldName, newName) ->
//          file.writeText(
//            file.readText()
//              .replace(
//                """  "url": "$oldName",""",
//                """  "url": "$newName",""",
//              )
//          )
//        }

        setOf(
          "256",
          "512",
        ).forEach {
          val checksum = file.computeChecksum("SHA-$it")
          file.resolveSibling(file.name + ".sha$it").writeText(checksum)
        }
      }

    logger.lifecycle("[$path] outputDir:${temporaryDir.invariantSeparatorsPath}")
  }
}
