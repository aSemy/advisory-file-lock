package demo

import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.random.Random

fun RandomAccessFile(file: Path, read: Boolean, write: Boolean): RandomAccessFile {
  return RandomAccessFile(
    file.toFile(),
    buildString {
      if (read) append("r")
      if (write) append("w")
    })
}

internal fun Path.readInt(): Int? {
  return readText().toIntOrNull() // ?: 0
//  return if (this.fileSize() == 0L) {
//    0
//  } else {
//    ByteBuffer.wrap(readBytes())
//      .getInt()
//  }
}

fun Path.writeInt(value: Int) {
  writeText(value.toString())
//  val bytes = ByteBuffer.allocate(Int.SIZE_BYTES)
//    .putInt(value)
//    .array()
//  writeBytes(bytes)
}

internal tailrec fun FileChannel.lockLenient(): FileLock {
  try {
    return lock()
  } catch (_: OverlappingFileLockException) {
    // ignore - process is already locked by this process
    Thread.sleep(Random.nextLong(25, 125))
  }
  return lockLenient()
}

//fun FileChannel.acquireReadLock(): FileLock {
//  while (true) {
//    try {
//      return lock()
//    } catch (_: OverlappingFileLockException) {
//      // ignore - process is already locked by this process
//      Thread.sleep(Random.nextLong(25, 125))
//    }
//  }
//}
//
//fun FileChannel.acquireWriteLock(): FileLock {
//  while (true) {
//    val lock = acquireReadLock()
//    try {
//      val readersCount = readInt()
//      if (readersCount == 0) {
//        return lock
//      } else {
//        println("Waiting for $readersCount readers to finish")
//        lock.release()
//        Thread.sleep(Random.nextLong(25, 125))
//      }
//    } catch (ex: Throwable) {
//      lock.release()
//      throw ex
//    }
//  }
//}
