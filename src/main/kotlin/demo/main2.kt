package demo

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.concurrent.thread
import kotlin.io.path.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


fun main(args: Array<String>) {
  val repetitions = args.firstOrNull()?.toIntOrNull() ?: 1000

  val workingDir = Path("demo2").createDirectories()
  val lockFile = workingDir.resolve("a.lock")
  if (!lockFile.exists()) {
    lockFile.createFile()
  }
  val dataFile = workingDir.resolve("data.txt")
  if (!dataFile.exists()) {
    dataFile.createFile()
    dataFile.writeText("0")
  }

  var c = 0

  repeat(5) { i ->
    thread(isDaemon = true, name = "reader$i") {
      FileReadWriteLock(lockFile).use { locker ->
        while (true) {
          Thread.sleep(i.seconds.inWholeMilliseconds)
          locker.withReadLock {
            val current = dataFile.readText().toInt()
            println("${Thread.currentThread().name}: current = $current")
            threadSleep((i + 1).seconds)
          }
//        RandomAccessFile(lockFile, read = true, write = true).use { lockFileAccess ->
//          val channel = lockFileAccess.channel
//          try {
//            channel.acquireReadLock().use {
//              channel.writeInt(channel.readInt() + 1)
//            }
//            val current = dataFile.readText().toInt()
//            println("${Thread.currentThread().name}: current = $current")
//            Thread.sleep(1.seconds.inWholeMilliseconds)
//          } finally {
//            channel.acquireReadLock().use {
//              channel.writeInt(channel.readInt() - 1)
//            }
//          }
//        }
          threadSleep((i + 1).seconds * 2)
        }
      }
    }
  }

  threadSleep(1.seconds)

  repeat(repetitions) {
    if (c > 100 && c % 127 == 0) {
      println("!!!!!!!!!!!!!!! TRIGGERING GC !!!!!!!!!!!!!!!!!!!!!")
      System.gc()
    }

    FileReadWriteLock(lockFile).use { locker ->
      locker.withWriteLock {
        val current = dataFile.readText().toInt()
        println("[writer] current = $current")
        dataFile.writeText((current + 1).toString())
        c++
        threadSleep(10.milliseconds)
      }
    }
  }

  println("c = $c")

  val endDataVal =
    FileReadWriteLock(lockFile).use { locker ->
      locker.withReadLock {
        dataFile.readText().toInt()
      }
    }
  println("endDataVal = $endDataVal")
}

fun FileChannel.readInt(position: Long = 0L): Int {
  // Ensure the file is large enough to hold an Int
  if (size() < Int.SIZE_BYTES) return 0

  val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
  read(buffer, position)
  buffer.flip()
  return buffer.getInt()
}

fun FileChannel.writeInt(value: Int, position: Long = 0L) {
  val buffer = ByteBuffer.allocate(Int.SIZE_BYTES).putInt(value)
  buffer.flip()
  write(buffer, position)
}

fun FileChannel.writeLong(value: Long, position: Long = 0L) {
  val buffer = ByteBuffer.allocate(Long.SIZE_BYTES).putLong(value)
  buffer.flip()
  write(buffer, position)
}

//fun RandomAccessFile.readInt(): Int {
//    return RandomAccessFile(toFile(), "r").use { raf ->
//        val channel = raf.channel
//        readInt(channel)
//    }
//}

//fun readString(channel: FileChannel) {
//
//  val data: MappedByteBuffer =
//    channel.map(FileChannel.MapMode.READ_WRITE, 0, 1024)
//
//  data
//
//  ByteArrayOutputStream().use { out ->
//    var bufferSize = 1024
//    if (bufferSize > channel.size()) {
//      bufferSize = channel.size().toInt()
//    }
//    val buff = ByteBuffer.allocate(bufferSize)
//
//    channel.read(buff)
//    out.write(buff.array(), 0, buff.position())
//      buff.clear()
//    while (channel.read(buff) > 0) {
//    }
//
//    val fileContent = String(out.toByteArray(), StandardCharsets.UTF_8)
//  }
//}
