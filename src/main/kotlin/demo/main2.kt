package demo

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.concurrent.thread
import kotlin.io.path.*
import kotlin.time.Duration.Companion.seconds


fun main(args: Array<String>) {
  val repetitions = args.firstOrNull()?.toIntOrNull() ?: 100_000

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
      while (true) {
        Thread.sleep((i + 1).seconds.inWholeMilliseconds)
        RandomAccessFile(lockFile, read = true, write = true).use { lockFileAccess ->
          val channel = lockFileAccess.channel
          try {
            channel.acquireReadLock().use {
              channel.writeInt(channel.readInt() + 1)
            }
            val current = dataFile.readText().toInt()
            println("${Thread.currentThread().name}: current = $current")
            Thread.sleep(1.seconds.inWholeMilliseconds)
          } finally {
            channel.acquireReadLock().use {
              channel.writeInt(channel.readInt() - 1)
            }
          }
        }
        Thread.sleep((i + 1).seconds.inWholeMilliseconds)
      }
    }
  }

  repeat(repetitions) {
    RandomAccessFile(lockFile, read = true, write = true).use { lockFileAccess ->
      val channel = lockFileAccess.channel
      channel.acquireWriteLock().use {
        val current = dataFile.readText().toInt()
        println("[writer] current = $current")
        dataFile.writeText((current + 1).toString())
        c++
      }
//      channel.lock().use { _ ->
////          println("Thread 1: Lock acquired.")
//        val current: Int = channel.readInt()
////          if (lockFileAccess.length() > 0) {
////            ByteBuffer.allocate(Int.SIZE_BYTES).let {
////              channel.read(it, 0)
////              it.rewind()
////                .getInt()
////            }
////          } else {
////            0
////          }
//
//        println("current = $current")
//
//        channel.writeInt(current + 1)
//        channel.force(false)
//        i++
//      }
    }
  }

  println("c = $c")
  val endDataVal =
    RandomAccessFile(lockFile, read = true, write = true).use { lockFileAccess ->
      val channel = lockFileAccess.channel
      channel.acquireReadLock().use { _ ->
        try {
          channel.writeInt(channel.readInt() + 1)
          dataFile.readText().toInt()
        } finally {
          channel.writeInt(channel.readInt() - 1)
        }
      }
    }
  println("endDataVal = $endDataVal")
}

fun FileChannel.readInt(): Int {
  // Ensure the file is large enough to hold an Int
  if (size() < Int.SIZE_BYTES) return 0

  val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
  read(buffer, 0)
  buffer.flip()
  return buffer.getInt()
}

fun FileChannel.writeInt(value: Int) {
  val buffer = ByteBuffer.allocate(Int.SIZE_BYTES).putInt(value)
  buffer.flip()
  write(buffer, 0)
}

//fun RandomAccessFile.readInt(): Int {
//    return RandomAccessFile(toFile(), "r").use { raf ->
//        val channel = raf.channel
//        readInt(channel)
//    }
//}
