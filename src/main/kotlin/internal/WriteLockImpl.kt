package dev.adamko.advisoryfilelock.internal

import dev.adamko.advisoryfilelock.LockAccess
import java.io.IOException
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.SocketChannel
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.exists
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

internal class WriteLockImpl(
  private val channel: FileChannel,
) : LockAccess.WriteLock() {

  private var lock: FileLock? = null

  override fun lock() {
    this.lock = acquireLock()
  }

  private tailrec fun acquireLock(): FileLock {
    val lock: FileLock = channel.lockLenient()
    try {
      val data = channel.readLockFileData()
      if (data.readers.isEmpty()) {
        return lock
      } else {
        logger.fine("[writer] Waiting for ${data.readers.size} readers to finish")
        refreshReaders()
        lock.release()
        Thread.sleep(Random.nextLong(25, 125))
      }
    } catch (ex: Throwable) {
      lock.release()
      throw ex
    }
    return acquireLock()
  }

  override fun unlock() {
    lock?.release()
  }

  private fun refreshReaders() {
    val storedData = channel.readLockFileData()
    val aliveReaders = storedData.readers.filterTo(sortedSetOf()) { isReaderAlive(it) }

    val actualData = LockFileData(aliveReaders)

    if (storedData != actualData) {
      val allReaders = actualData.readers union storedData.readers
      val activeAndStoredReaders = aliveReaders union storedData.readers
      val mismatch = allReaders subtract activeAndStoredReaders
      logger.fine("stored readers != alive readers\n\tmismatch:$mismatch\n\tstored:${storedData}\n\tactual:${actualData}")
      channel.writeLockFileData(actualData)
    }
  }

  private fun isReaderAlive(socketPath: Path): Boolean {
    if (!socketPath.exists()) return false

    var attempts = 0
    val maxAttempts = 5
    while (++attempts < maxAttempts) {

      try {
        val client = SocketChannel.open(UnixDomainSocketAddress.of(socketPath))
        client.configureBlocking(false)
        client.finishConnect()
        val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
          .putInt(1)
          .flip()
        while (buffer.hasRemaining()) {
          client.write(buffer)
        }
        buffer.clear()
        client.read(buffer)
        val response = buffer.getInt()

        val isAlive = response == 1
        if (!isAlive) {
          logger.fine("[writer] Reader $socketPath is dead")
        }
        return isAlive
      } catch (ex: IOException) {
        logger.warning("[writer] failed to check reader $socketPath: $ex")
        threadSleep(500.milliseconds)
        continue
      }
    }
    logger.warning("[writer] failed to check reader $socketPath after $attempts attempts")

    return false
  }

  companion object {
    private val logger: Logger = Logger.getLogger(LockAccess.WriteLock::class.qualifiedName)
  }
}
