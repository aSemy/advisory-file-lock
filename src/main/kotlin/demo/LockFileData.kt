package demo

import java.nio.file.Path
import kotlinx.serialization.Serializable
import serialization.PathAsStringSerializer

@Serializable
internal data class LockFileData(
  val readers: Set<@Serializable(PathAsStringSerializer::class) Path>,
) {
  fun addReader(reader: Path): LockFileData =
    copy(readers = readers union setOf(reader))

  fun removeReader(reader: Path): LockFileData =
    copy(readers = readers - setOf(reader))

//  companion object {
//    val descriptorChecksum: String by lazy {
//      computeChecksum(serializer().descriptor)
//    }
//  }
}
