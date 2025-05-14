package dev.adamko.lokka

import dev.adamko.lokka.internal.*
import java.lang.ref.WeakReference
import java.nio.channels.FileChannel
import java.nio.file.Path
import kotlin.io.path.absolute

/**
 * Manage read lock on a file.
 *
 * Can be locked/unlocked multiple times.
 */
internal class ReadLock(
  private val channel: FileChannel,
  private val id: String = randomAlphaNumericString(),
  socketDir: Path,
) : LockAccess {

  private val socketFile: Path =
    socketDir.resolve(id)
      .absolute()

  init {
    PingListener(
      id = id,
      lockRef = WeakReference(this),
      socketFile = socketFile,
    )

    // TODO delete socketPath on exit.
    //      Currently I'm testing and it's nice to have a bad implementation to see what happens when the sockets aren't removed.
  }

  override fun lock() {
    channel.lockLenient().use { _ ->
      println("[reader $id] Locking")
      val data = channel.readLockFileData()
      val newData = data.addReader(socketFile)
      channel.writeLockFileData(newData)
    }
  }

  override fun unlock() {
    channel.lockLenient().use { _ ->
      println("[reader $id] Unlocking")
//      pingListener?.interrupt()
//      pingListener = null
      val data = channel.readLockFileData()
      if (socketFile !in data.readers) {
        System.err.println("[reader $id] lock file data does not contain $socketFile")
      } else {
        val newData = data.removeReader(socketFile)
        channel.writeLockFileData(newData)
      }
    }
  }

  override fun close() {
    unlock()
  }
}
