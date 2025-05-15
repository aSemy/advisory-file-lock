package dev.adamko.advisoryfilelock.internal.serialization

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

internal val binaryFormat: BinaryFormat = BinaryFormat()

/** [`https://github.com/Kotlin/kotlinx.internal.serialization/blob/master/docs/formats.md#efficient-binary-format`](https://github.com/Kotlin/kotlinx.internal.serialization/blob/master/docs/formats.md#efficient-binary-format) */
internal class BinaryFormat {

  fun <T> encodeToByteArray(serializer: BinarySerializer<T>, value: T): ByteArray {
    val outputStream = ByteArrayOutputStream()
    val output = DataOutputStream(outputStream)
    val encoder = DataOutputEncoder(output)
    encoder.encodeSerializableValue(serializer, value)
    return outputStream.toByteArray()
  }

  fun <T> decodeFromByteArray(
    deserializer: BinarySerializer<T>,
    bytes: ByteArray
  ): T {
    val inputStream = bytes.inputStream()
    val input = DataInputStream(inputStream)
    val decoder = DataInputDecoder(input)
    return decoder.decodeSerializableValue(deserializer)
  }
}

internal interface BinarySerializer<T : Any?> {
  fun deserialize(decoder: DataInputDecoder): T
  fun serialize(encoder: DataOutputEncoder, value: T)
}

internal class DataOutputEncoder(
  private val output: DataOutputStream,
) {
  fun encodeBoolean(value: Boolean): Unit = output.writeBoolean(value)
  fun encodeByte(value: Byte): Unit = output.writeByte(value.toInt())
  fun encodeChar(value: Char): Unit = output.writeChar(value.code)
  fun encodeDouble(value: Double): Unit = output.writeDouble(value)
  fun encodeEnum(index: Int): Unit = output.writeInt(index)
  fun encodeFloat(value: Float): Unit = output.writeFloat(value)
  fun encodeInt(value: Int): Unit = output.writeInt(value)
  fun encodeLong(value: Long): Unit = output.writeLong(value)
  fun encodeShort(value: Short): Unit = output.writeShort(value.toInt())
  fun encodeString(value: String) {
    val bytes = value.toByteArray(Charsets.UTF_8)
    encodeCompactSize(bytes.size)
    output.write(bytes)
  }

  fun beginCollection(collectionSize: Int) {
    encodeCompactSize(collectionSize)
  }

  fun encodeNull(): Unit = encodeBoolean(false)
  fun encodeNotNullMark(): Unit = encodeBoolean(true)

  fun <T> encodeSerializableValue(serializer: BinarySerializer<T>, value: T) {
    serializer.serialize(this, value)
  }

  private fun encodeCompactSize(value: Int) {
    if (value < 0xff) {
      output.writeByte(value)
    } else {
      output.writeByte(0xff)
      output.writeInt(value)
    }
  }
}


internal class DataInputDecoder(
  private val input: DataInputStream,
  private var elementsCount: Int = 0,
) {

  private var elementIndex = 0

  fun decodeBoolean(): Boolean = input.readBoolean()
  fun decodeByte(): Byte = input.readByte()
  fun decodeChar(): Char = input.readChar()
  fun decodeDouble(): Double = input.readDouble()
  fun decodeEnum(): Int = input.readInt()
  fun decodeFloat(): Float = input.readFloat()
  fun decodeInt(): Int = input.readInt()
  fun decodeLong(): Long = input.readLong()
  fun decodeShort(): Short = input.readShort()
  fun decodeString(): String {
    val size = decodeCompactSize()
    val bytes = ByteArray(size)
    input.readFully(bytes)
    return bytes.toString(Charsets.UTF_8)
  }

  fun decodeElementIndex(): Int {
    if (elementIndex == elementsCount) return -1 // CompositeDecoder.DECODE_DONE
    return elementIndex++
  }

  fun beginStructure(elementsCount: Int) =
    DataInputDecoder(input, elementsCount)

//  fun decodeSequentially(): Boolean = true

  fun decodeCollectionSize(): Int =
    decodeCompactSize().also { elementsCount = it }

  fun decodeNotNullMark(): Boolean = decodeBoolean()

  fun <T> decodeSerializableValue(
    deserializer: BinarySerializer<T>,
  ): T {
    return deserializer.deserialize(this)
  }

  private fun decodeCompactSize(): Int {
    val byte = input.readByte().toInt() and 0xff
    return if (byte < 0xff) byte else input.readInt()
  }
}
