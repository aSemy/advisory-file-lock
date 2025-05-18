# Advisory File Lock

Kotlin/JVM library for managing advisory file locks.

Uses a lock file to control read/write access to critical sections across threads or processes.

### Features

- Multiple concurrent readers are permitted.
- A read-lock can only be obtained if there are no write-locks.
- Only a single writer is permitted
- A write-lock can only be obtained if there are no other locks (no other readers or writers have locks).

The lock is only advisory, and cannot prevent misuse (like a writing data while under a read-only lock).

The performance is not good: Use when accuracy is more important than speed.

Locks can communicate with each other using unix domain sockets.
The sockets are used to verify the processes holding the locks are still alive
(so if a process unexpectedly exits the locks are not held indefinitely).

### Usage

Artifacts are published to GitHub Releases.

```kotlin
// build.gradle.kts


repositories {
  ivy("https://github.com/") {
    name = "GitHub Releases"
    patternLayout {
      setM2compatible(true)
      artifact("[organisation]/releases/download/v[revision]/[module]-[revision].[ext]")
    }
    metadataSources {
      gradleMetadata()
    }
  }
}
```

Requires Java 17:
Uses [UnixDomainSocketAddress](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/UnixDomainSocketAddress.html).

### Contributing

Requires Java 21 for running Gradle and compiling.
