package dev.adamko.advisoryfilelock.demo

import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Path
import kotlin.random.Random
import kotlin.time.Duration

internal tailrec fun FileChannel.lockLenient(): FileLock {
  try {
    return lock()
  } catch (_: OverlappingFileLockException) {
    // ignore - process is already locked by this process
    Thread.sleep(Random.nextLong(25, 125))
  }
  return lockLenient()
}


internal fun RandomAccessFile(file: Path, read: Boolean, write: Boolean): RandomAccessFile {
  return RandomAccessFile(
    file.toFile(),
    buildString {
      if (read) append("r")
      if (write) append("w")
    })
}

internal fun threadSleep(duration: Duration) {
  Thread.sleep(duration.inWholeMilliseconds)
}
