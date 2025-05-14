package dev.adamko.lokka.internal

import dev.adamko.lokka.internal.serialization.binaryFormat
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import kotlin.random.Random
import kotlin.time.Duration

//internal fun RandomAccessFile(file: Path, read: Boolean, write: Boolean): RandomAccessFile {
//  return RandomAccessFile(
//    file.toFile(),
//    buildString {
//      if (read) append("r")
//      if (write) append("w")
//    })
//}

//internal fun Path.readInt(): Int? {
//  return readText().toIntOrNull()
//}

//fun Path.writeInt(value: Int) {
//  writeText(value.toString())
//}

internal tailrec fun FileChannel.lockLenient(): FileLock {
  try {
    return lock()
  } catch (_: OverlappingFileLockException) {
    // ignore - process is already locked by this process
    Thread.sleep(Random.nextLong(25, 125))
  }
  return lockLenient()
}


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
  val encoded = binaryFormat.encodeToByteArray(LockFileDataSerializer, data)
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

  val data = binaryFormat.decodeFromByteArray(LockFileDataSerializer, bytes)
  return data
}
