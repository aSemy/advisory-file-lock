package dev.adamko.lokka

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
    return ReadLock(
      channel = accessFile.channel,
      socketDir = socketDir,
    )
  }

  fun writeLock(): LockAccess {
    return WriteLock(accessFile.channel)
  }

  override fun close() {
    accessFile.close()
  }

  companion object {
    private val defaultSocketDir: Path by lazy {
      val dir = System.getProperty("dev.adamko.lokka.socketDir")
        ?: System.getProperty("java.io.tmpdir")

      Path(dir).resolve("frwl")
    }
  }
}
