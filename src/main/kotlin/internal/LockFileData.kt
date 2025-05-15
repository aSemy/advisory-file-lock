package dev.adamko.advisoryfilelock.internal

import java.nio.file.Path

internal data class LockFileData(
  val readers: Set<Path>,
) {
  fun addReader(reader: Path): LockFileData =
    copy(readers = readers union setOf(reader))

  fun removeReader(reader: Path): LockFileData =
    copy(readers = readers - setOf(reader))
}
