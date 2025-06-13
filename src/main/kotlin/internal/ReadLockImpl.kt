package dev.adamko.advisoryfilelock.internal

import dev.adamko.advisoryfilelock.LockAccess
import java.lang.ref.WeakReference
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.absolute

/**
 * Manage read lock on a file.
 *
 * Can be locked/unlocked multiple times.
 */
internal class ReadLockImpl(
  private val name: String,
  private val channel: FileChannel,
  socketDir: Path,
  private val id: String = randomAlphaNumericString(),
) : LockAccess.ReadLock() {

  private val socketFile: Path =
    socketDir.resolve(id).absolute()

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
      logger.fine("[reader $id] Locking")
      val data = channel.readLockFileData()
      val newData = data.addReader(
        LockFileData.Reader(
          id = name,
          socketPath = socketFile,
        )
      )
      channel.writeLockFileData(newData)
    }
  }

  override fun unlock() {
    channel.lockLenient().use { _ ->
      logger.fine("[reader $id] Unlocking")
//      pingListener?.interrupt()
//      pingListener = null
      val data = channel.readLockFileData()
      if (data.readers.none { it.socketPath == socketFile }) {
        logger.warning("[reader $id] lock file data does not contain $socketFile")
      } else {
        val newData = data.removeReader(socketFile)
        channel.writeLockFileData(newData)
      }
    }
  }

  companion object {
    private val logger: Logger = Logger.getLogger(LockAccess.ReadLock::class.qualifiedName)
  }
}
