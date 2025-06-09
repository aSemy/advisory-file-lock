package dev.adamko.advisoryfilelock.internal

import java.nio.file.Path

internal data class LockFileData(
  val readers: Set<Reader>,
) {
  fun addReader(reader: Reader): LockFileData =
    copy(readers = readers union setOf(reader))

  fun removeReader(readerSocketPath: Path): LockFileData =
    copy(readers = readers.filter { it.socketPath != readerSocketPath }.toSet())

  data class Reader(
    /**
     * User-friendly identifier.
     *
     * Used in log and error messages to debug locks.
     */
    val id: String,
    val socketPath: Path,
  )
}
