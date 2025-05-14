package dev.adamko.lokka.internal

import dev.adamko.lokka.internal.serialization.BinarySerializer
import dev.adamko.lokka.internal.serialization.DataInputDecoder
import dev.adamko.lokka.internal.serialization.DataOutputEncoder
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString

//sealed interface LockFileData {
//
//  fun encode(): ByteArray
//
//  data class V1(
//    val readers: Set<Path>,
//  ) : LockFileData
//}

//@Serializable
internal data class LockFileData(
  val readers: Set<Path>,
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

//private fun computeChecksum(descriptor: SerialDescriptor): String {
//  val messageDigest = MessageDigest.getInstance("SHA-256")
//
//  DigestOutputStream(nullOutputStream(), messageDigest).use { digestStream ->
//    fun hash(value: String): Unit =
//      digestStream.write(value.toByteArray())
//
//    fun hash(value: Boolean): Unit =
//      digestStream.write(if (value) 1 else 0)
//
//    fun hash(value: Int) {
//      digestStream.write(value)
//    }
//
//    tailrec fun updateDigestWithDescriptor(
//      queue: MutableList<SerialDescriptor> = mutableListOf(descriptor),
//      visited: MutableSet<SerialDescriptor> = mutableSetOf(),
//    ) {
//      val desc = queue.removeFirstOrNull() ?: return
//
//      // Update with the name and kind of the descriptor
//      hash(desc.serialName)
//      hash(desc.kind.hashCode())
//      hash(desc.isNullable)
//      hash(desc.isInline)
//      desc.annotations.forEach { annotation ->
//        hash(annotation.hashCode())
//      }
//
//      hash(desc.elementsCount)
//      repeat(desc.elementsCount) { index ->
//        hash(desc.getElementName(index))
//        hash(desc.isElementOptional(index))
//        desc.getElementAnnotations(index).forEach { annotation ->
//          hash(annotation.hashCode())
//        }
//
//        val element = desc.getElementDescriptor(index)
//        if (visited.add(element)) {
//          queue.add(element)
//        }
//      }
//
//      updateDigestWithDescriptor(queue, visited)
//    }
//
//    updateDigestWithDescriptor()
//  }
//
//  return Base64.getEncoder().encodeToString(messageDigest.digest())
//}
