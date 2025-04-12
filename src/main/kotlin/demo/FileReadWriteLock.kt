package demo

import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Path
import kotlin.random.Random

internal class FileReadWriteLock(
  private val lockFile: Path,
) : AutoCloseable {

  private val accessFile: RandomAccessFile =
    RandomAccessFile(lockFile.toFile(), "rw")

  fun isOpen(): Boolean = accessFile.channel.isOpen

  fun readLock(): LockAccess {
    return ReadLock(accessFile)
  }

  fun writeLock(): LockAccess {
    return WriteLock(accessFile)
  }

  override fun close() {
    accessFile.close()
  }

  internal sealed interface LockAccess : AutoCloseable {
    fun lock()
    fun unlock()
    override fun close(): Unit = unlock()
  }

  private class ReadLock(
    lfa: RandomAccessFile,
  ) : LockAccess {
    private val channel: FileChannel = lfa.channel

    override fun lock() {
      channel.lockLenient().use { _ ->
        val current = channel.readInt()
        channel.writeInt(current + 1)
      }
    }

    override fun unlock() {
      channel.lockLenient().use { _ ->
        val current = channel.readInt()
        channel.writeInt(current - 1)
      }
    }

    override fun close() {
      unlock()
    }
  }

  private class WriteLock(
    raf: RandomAccessFile,
  ) : LockAccess {
    private val channel: FileChannel = raf.channel
    private var lock: FileLock? = null

    override fun lock() {
      while (true) {
        val lock: FileLock = channel.lockLenient()
        try {
          val readersCount = channel.readInt()
          if (readersCount == 0) {
            return
          } else {
            println("Waiting for $readersCount readers to finish")
            lock.release()
            Thread.sleep(Random.nextLong(25, 125))
          }
        } catch (ex: Throwable) {
          lock.release()
          throw ex
        }
      }
    }

    override fun unlock() {
      lock?.release()
    }

    override fun close() {
      unlock()
    }
  }
}
