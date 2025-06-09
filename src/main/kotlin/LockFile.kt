package dev.adamko.advisoryfilelock

import dev.adamko.advisoryfilelock.internal.ReadLockImpl
import dev.adamko.advisoryfilelock.internal.WriteLockImpl
import java.io.RandomAccessFile
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists

/**
 * A lock file that can be used to synchronize multiple processes.
 *
 * The lock file is a regular file, which is opened for read/write access.
 *
 * The lock file is locked for the duration of the [LockFile] instance.
 *
 * @param name Human-readable name for log and error messages.
 * @param lockFile the path to the lock file.
 * @param socketDir the directory to store the lock file's socket file
 * (used for interprocess communication, to verify the lock is still held).
 */
class LockFile(
  private val name: String,
  lockFile: Path,
  private val socketDir: Path = defaultSocketDir,
) : AutoCloseable {

  constructor(
    name: String,
    path: String,
    socketDir: Path = defaultSocketDir,
  ) : this(
    name = name,
    lockFile = Path(path),
    socketDir = socketDir,
  )

  init {
    if (!lockFile.exists()) {
      lockFile.createFile()
    }
    socketDir.createDirectories()
  }

  private val accessFile: RandomAccessFile =
    RandomAccessFile(lockFile.toFile(), "rw")

//  fun isOpen(): Boolean = accessFile.channel.isOpen

  fun readLock(): LockAccess.ReadLock {
    return ReadLockImpl(
      name = name,
      channel = accessFile.channel,
      socketDir = socketDir,
    )
  }

  fun writeLock(): LockAccess.WriteLock {
    return WriteLockImpl(
      channel = accessFile.channel,
    )
  }

  override fun close() {
    accessFile.close()
  }

  companion object {
    private val defaultSocketDir: Path by lazy {
      val socketDir = System.getProperty("dev.adamko.advisoryfilelock.socketDir")
      if (socketDir != null) {
        Path(socketDir)
      } else {
        val tmpDir = System.getProperty("java.io.tmpdir")
        Path(tmpDir).resolve("frwl")
      }
    }
  }
}
