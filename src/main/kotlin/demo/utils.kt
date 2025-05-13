package demo

import java.io.ByteArrayOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.random.Random
import kotlin.time.Duration
import serialization.kxsBinary

internal fun RandomAccessFile(file: Path, read: Boolean, write: Boolean): RandomAccessFile {
  return RandomAccessFile(
    file.toFile(),
    buildString {
      if (read) append("r")
      if (write) append("w")
    })
}

//internal fun Path.readInt(): Int? {
//  return readText().toIntOrNull()
//}

fun Path.writeInt(value: Int) {
  writeText(value.toString())
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

internal fun threadSleep(duration: Duration) {
  Thread.sleep(duration.inWholeMilliseconds)
}

internal fun randomAlphaNumericString(size: Int = 16): String {
  val chars: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
  return buildString {
    repeat(size) {
      append(chars.random())
    }
  }
}



internal fun FileChannel.writeLockFileData(data: LockFileData) {
  val encoded = kxsBinary.encodeToByteArray(LockFileData.serializer(), data)
  write(ByteBuffer.wrap(encoded), 0)
}

internal fun FileChannel.readLockFileData(): LockFileData {
  if (size() == 0L) {
    println("Lock file is empty, returning empty data")
    return LockFileData(sortedSetOf())
  }
  position(0)
  val bytes = ByteArrayOutputStream().use { os ->
    val buf = ByteBuffer.allocate(1024)
    while (read(buf) > 0) {
      os.write(buf.array(), 0, buf.limit())
      buf.clear()
    }
    os.toByteArray()
  }

  val data = kxsBinary.decodeFromByteArray(LockFileData.serializer(), bytes)
  return data
}
