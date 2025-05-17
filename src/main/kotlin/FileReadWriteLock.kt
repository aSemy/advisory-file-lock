package dev.adamko.advisoryfilelock

import dev.adamko.advisoryfilelock.internal.ReadLockImpl
import dev.adamko.advisoryfilelock.internal.WriteLockImpl
import java.io.RandomAccessFile
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories


class FileReadWriteLock(
  lockFile: Path,
  private val socketDir: Path = defaultSocketDir,
) : AutoCloseable {

  init {
    socketDir.createDirectories()
  }

  private val accessFile: RandomAccessFile =
    RandomAccessFile(lockFile.toFile(), "rw")

//  fun isOpen(): Boolean = accessFile.channel.isOpen

  fun readLock(): LockAccess {
    return ReadLockImpl(
      channel = accessFile.channel,
      socketDir = socketDir,
    )
  }

  fun writeLock(): LockAccess {
    return WriteLockImpl(
      channel = accessFile.channel,
    )
  }

  override fun close() {
    accessFile.close()
  }

  companion object {
    private val defaultSocketDir: Path by lazy {
      val dir = System.getProperty("dev.adamko.advisoryfilelock.socketDir")
        ?: System.getProperty("java.io.tmpdir")

      Path(dir).resolve("frwl")
    }
  }
}
