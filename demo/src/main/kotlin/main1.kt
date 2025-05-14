package dev.adamko.lokka.demo

import kotlin.concurrent.thread
import kotlin.io.path.*

fun main(args: Array<String>) {
  val repetitions = args.firstOrNull()?.toIntOrNull() ?: 1_000

  val workingDir = Path("demo-data/main1").createDirectories()
  val lockFile = workingDir.resolve("a.lock")
  if (!lockFile.exists()) {
    lockFile.createFile()
  }

//  lockFile.writeInt(0)
  var i = 0

  // Thread 1
  val thread1 = thread(name = "Thread 1") {
    repeat(repetitions) {
      RandomAccessFile(lockFile, read = true, write = true).use { lockFileAccess ->
        val channel = lockFileAccess.channel
        channel.lockLenient().use { _ ->
          val current: Int = channel.readInt()
          println("[Thread 1] current = $current")
          channel.writeInt(current + 1)
          channel.force(false)
          i++
        }
      }
    }
  }

  // Delay, to let thread1 grab the lock first
  Thread.sleep(500)

  // Thread 2
  val thread2 = thread(name = "Thread 2") {
    repeat(repetitions) {
      RandomAccessFile(lockFile, read = true, write = true).use { lockFileAccess ->
        val channel = lockFileAccess.channel
        channel.lockLenient().use { _ ->
          i++
          val current: Int = channel.readInt()
          println("[Thread 2] current = $current")
          channel.writeInt(current + 1)
          channel.force(false)
        }
      }
    }
  }

  thread1.join()
  thread2.join()

  println("i = $i")
  println("lockFile = ${lockFile.readText().toIntOrNull()}")
}
