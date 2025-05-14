package dev.adamko.lokka.internal.serialization


import dev.adamko.lokka.internal.LockFileData
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString


internal object LockFileDataSerializer : BinarySerializer<LockFileData> {
  private const val VERSION: Int = 1

  override fun serialize(encoder: DataOutputEncoder, value: LockFileData) {
    encoder.apply {
      encodeInt(VERSION)
      beginCollection(value.readers.size)
      value.readers.forEach { reader ->
        encodeString(reader.invariantSeparatorsPathString)
      }
    }
  }

  override fun deserialize(decoder: DataInputDecoder): LockFileData {
    val actualVersion = decoder.decodeInt()
    if (actualVersion != VERSION) {
      error("LockFileData version $actualVersion is not supported. Expected version $VERSION.")
    }
    val readersSize = decoder.decodeCollectionSize()
    val readers = decoder.beginStructure(readersSize).run {
      buildSet {
        repeat(readersSize) {
          val path = decodeString()
          add(Path(path))
        }
      }
    }
    return LockFileData(readers)
  }
}
