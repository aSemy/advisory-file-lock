package dev.adamko.advisoryfilelock.internal.serialization

import dev.adamko.advisoryfilelock.internal.LockFileData
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString

internal object LockFileDataSerializer : BinarySerializer<LockFileData> {

  private const val HEADER: String = "dev.adamko.advisoryfilelock.LockFileData"
  private const val VERSION: Int = 2

  override fun serialize(encoder: DataOutputEncoder, value: LockFileData) {
    encoder.apply {
      encodeString(HEADER)
      encodeInt(VERSION)
      beginCollection(value.readers.size)
      value.readers.forEach { reader ->
        encodeString(reader.id)
        encodeString(reader.socketPath.invariantSeparatorsPathString)
      }
    }
  }

  override fun deserialize(decoder: DataInputDecoder): LockFileData {
    val header = decoder.decodeString()
    require(header == HEADER) {
      "Lock file does not have expected header. Expected: $HEADER, actual: $header"
    }

    return when (val actualVersion = decoder.decodeInt()) {
      1    -> deserializeV1(decoder)
      2    -> deserializeV2(decoder)
      else -> error("LockFileData version $actualVersion is not supported.")
    }
  }

  private fun deserializeV1(decoder: DataInputDecoder): LockFileData {
    val readersSize = decoder.decodeCollectionSize()
    val readers = buildSet {
//      decoder.beginStructure().run {
      repeat(readersSize) {
        val path = Path(decoder.decodeString())
        add(LockFileData.Reader("unknown", path))
      }
//      }
    }
    return LockFileData(readers)
  }

  private fun deserializeV2(decoder: DataInputDecoder): LockFileData {
    val readersSize = decoder.decodeCollectionSize()
    val readers = buildSet {
//      decoder.beginStructure().run {
      repeat(readersSize) {
        val reader = decoder.decodeSerializableValue(ReaderSerializer)
        add(reader)
      }
//      }
    }
    return LockFileData(readers)
  }
}

private object ReaderSerializer : BinarySerializer<LockFileData.Reader> {
  override fun serialize(encoder: DataOutputEncoder, value: LockFileData.Reader) {
    encoder.apply {
      encodeString(value.id)
      encodeString(value.socketPath.invariantSeparatorsPathString)
    }
  }

  override fun deserialize(decoder: DataInputDecoder): LockFileData.Reader {
    return LockFileData.Reader(
      id = decoder.decodeString(),
      socketPath = Path(decoder.decodeString()),
    )
  }
}
